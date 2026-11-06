import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import {
    useAlertasAnaliticasQuery,
    useAuditoriaQuery,
    useEstadoStreamsQuery,
    useMovimientosQuery,
    useResumenAnaliticasQuery,
    useTopProductosQuery,
} from '../data/analiticas.queries';
import {
    useMetricasDiariasQuery,
    useRabbitOverviewQuery,
    useUltimosEventosQuery,
} from '../data/dashboard.queries';
import {
    DesgloseTipo,
    DiaAgrupado,
    EventoReciente,
    KpiMes,
    MesAgrupado,
    TopProducto,
} from '../data/dashboard.types';

const MESES_CORTOS = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];

const MESES_LARGOS = [
    'enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio',
    'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre',
];

const RK_CREADOS = 'pedido.crear';
const RK_ACEPTADOS = 'pedido.creado';
const RK_RESERVADOS = 'pedido.stock.reservado';
const RK_FALLIDOS_STOCK = 'pedido.stock.fallido';
const RK_CANCELADOS = 'pedido.estado.cancelado';
const RK_DLQ = 'pedidos.failed';

const MESES_VENTANA = 12;
const ALTO_BARRAS_PX = 150;
const LIMITE_ULTIMOS = 20;

function aIso(fecha: Date): string
{
    const anio = fecha.getFullYear();
    const mes = String(fecha.getMonth() + 1).padStart(2, '0');
    const dia = String(fecha.getDate()).padStart(2, '0');

    return `${anio}-${mes}-${dia}`;
}

function claveDeMes(fecha: Date): string
{
    return aIso(fecha).slice(0, 7);
}

@Component({
    selector: 'app-dashboard-page',
    standalone: true,
    imports: [DatePipe, DecimalPipe, CurrencyPipe],
    templateUrl: './dashboard-page.html',
})
export class DashboardPage
{
    protected readonly vista = signal<'anual' | 'mensual'>('anual');
    protected readonly mesSeleccionado = signal<string | null>(null);

    private readonly periodoRanking = computed(() =>
    {
        const ahora = new Date();
        const clave = this.mesSeleccionado();

        if (this.vista() === 'mensual' && clave !== null)
        {
            const [anio, mes] = clave.split('-').map(Number);
            const desde = new Date(anio, mes - 1, 1);
            const hasta = new Date(anio, mes, 0);

            return { desde: aIso(desde), hasta: aIso(hasta) };
        }

        return {
            desde: aIso(new Date(
                ahora.getFullYear(),
                ahora.getMonth() - (MESES_VENTANA - 1),
                1
            )),
            hasta: aIso(ahora),
        };
    });

    // RabbitMQ: se mantienen las fuentes actuales de estas secciones.
    private readonly diariasQuery = useMetricasDiariasQuery(MESES_VENTANA);
    private readonly ultimosQuery = useUltimosEventosQuery(LIMITE_ULTIMOS);
    private readonly overviewQuery = useRabbitOverviewQuery();

    // Analytics: consultas del servicio de analíticas.
    private readonly resumenAnaliticasQuery = useResumenAnaliticasQuery();
    private readonly alertasAnaliticasQuery = useAlertasAnaliticasQuery();
    private readonly rankingQuery = useTopProductosQuery(
        () => this.periodoRanking()
    );
    private readonly movimientosQuery = useMovimientosQuery(
        () => this.periodoRanking()
    );
    private readonly auditoriaQuery = useAuditoriaQuery(
        () => this.periodoRanking(),
        LIMITE_ULTIMOS
    );
    private readonly estadoStreamsQuery = useEstadoStreamsQuery();

    protected readonly puntos = computed(() =>
        this.diariasQuery.data() ?? []
    );

    protected readonly mesesAgrupados = computed<MesAgrupado[]>(() =>
    {
        const hoy = new Date();
        const lista: MesAgrupado[] = [];

        for (let i = MESES_VENTANA - 1; i >= 0; i--)
        {
            const mes = new Date(hoy.getFullYear(), hoy.getMonth() - i, 1);
            const clave = claveDeMes(mes);

            lista.push({
                clave,
                etiqueta: `${MESES_CORTOS[mes.getMonth()]} ${String(mes.getFullYear()).slice(2)}`,
                total: this.pedidosDeMes(clave),
            });
        }

        return lista;
    });

    protected readonly maxMes = computed(() =>
        Math.max(1, ...this.mesesAgrupados().map((mes) => mes.total))
    );

    protected readonly tituloMesSeleccionado = computed(() =>
    {
        const clave = this.mesSeleccionado();

        if (clave === null)
            return '';

        const [anio, mes] = clave.split('-').map(Number);

        return `${MESES_LARGOS[mes - 1]} ${anio}`;
    });

    protected readonly diasDelMes = computed<DiaAgrupado[]>(() =>
    {
        const clave = this.mesSeleccionado();

        if (clave === null)
            return [];

        const [anio, mes] = clave.split('-').map(Number);
        const ultimoDia = new Date(anio, mes, 0).getDate();
        const porDia = new Map<string, number>();

        for (const punto of this.puntos())
        {
            if (
                punto.fecha.startsWith(clave) &&
                punto.routingKey === RK_CREADOS
            )
            {
                porDia.set(
                    punto.fecha,
                    (porDia.get(punto.fecha) ?? 0) + punto.total
                );
            }
        }

        const lista: DiaAgrupado[] = [];

        for (let dia = 1; dia <= ultimoDia; dia++)
        {
            const fecha = `${clave}-${String(dia).padStart(2, '0')}`;

            lista.push({
                fecha,
                etiqueta: String(dia),
                total: porDia.get(fecha) ?? 0,
            });
        }

        return lista;
    });

    protected readonly maxDia = computed(() =>
        Math.max(1, ...this.diasDelMes().map((dia) => dia.total))
    );

    protected readonly desgloseMes = computed<DesgloseTipo[]>(() =>
    {
        const clave = this.mesSeleccionado();

        if (clave === null)
            return [];

        const totales = this.totalesMes(clave);
        const totalMes = [...totales.values()]
            .reduce((acc, total) => acc + total, 0) || 1;

        return [...totales.entries()]
            .map(([routingKey, total]) => ({
                routingKey,
                total,
                porcentaje: Math.round((total / totalMes) * 1000) / 10,
            }))
            .sort((a, b) => b.total - a.total);
    });

    protected readonly totalMesSeleccionado = computed(() =>
        this.diasDelMes().reduce((acc, dia) => acc + dia.total, 0)
    );

    protected readonly kpis = computed<KpiMes[]>(() =>
    {
        const ahora = new Date();
        const claveActual = claveDeMes(ahora);
        const claveAnterior = claveDeMes(
            new Date(ahora.getFullYear(), ahora.getMonth() - 1, 1)
        );

        const actual = this.totalesMes(claveActual);
        const anterior = this.totalesMes(claveAnterior);

        const totalActual = [...actual.values()]
            .reduce((acc, total) => acc + total, 0);

        const totalAnterior = [...anterior.values()]
            .reduce((acc, total) => acc + total, 0);

        const creados = actual.get(RK_CREADOS) ?? 0;
        const aceptados = actual.get(RK_ACEPTADOS) ?? 0;

        const tasa = creados > 0
            ? Math.round((aceptados / creados) * 1000) / 10
            : 0;

        return [
            this.kpi('Pedidos creados', creados, anterior.get(RK_CREADOS) ?? 0, 'Jobs encolados'),
            this.kpi('Pedidos aceptados', aceptados, anterior.get(RK_ACEPTADOS) ?? 0, 'Listos para entrega'),
            {
                etiqueta: 'Tasa de aceptación',
                valor: `${tasa.toLocaleString('es-CL', { maximumFractionDigits: 1 })}%`,
                delta: null,
                detalle: 'Aceptados / creados del mes',
            },
            this.kpi('Reservas de stock', actual.get(RK_RESERVADOS) ?? 0, anterior.get(RK_RESERVADOS) ?? 0, 'Stock confirmado'),
            this.kpi('Fallos de stock', actual.get(RK_FALLIDOS_STOCK) ?? 0, anterior.get(RK_FALLIDOS_STOCK) ?? 0, 'Sin stock disponible'),
            this.kpi('Cancelaciones', actual.get(RK_CANCELADOS) ?? 0, anterior.get(RK_CANCELADOS) ?? 0, 'Con compensación'),
            this.kpi('Fallos transitorios', actual.get(RK_DLQ) ?? 0, anterior.get(RK_DLQ) ?? 0, 'Enviados a DLQ'),
            this.kpi('Total eventos', totalActual, totalAnterior, 'Todos los tipos'),
        ];
    });

    protected readonly resumenAnaliticas = computed(() =>
        this.resumenAnaliticasQuery.data() ?? null
    );

    protected readonly alertasAnaliticas = computed(() =>
        this.alertasAnaliticasQuery.data() ?? []
    );

    protected readonly alertasAnaliticasCargando = computed(() =>
        this.alertasAnaliticasQuery.isPending()
    );

    protected readonly movimientos = computed(() =>
        this.movimientosQuery.data() ?? []
    );

    protected readonly movimientosCargando = computed(() =>
        this.movimientosQuery.isPending()
    );

    protected readonly auditoria = computed(() =>
        this.auditoriaQuery.data() ?? []
    );

    protected readonly auditoriaCargando = computed(() =>
        this.auditoriaQuery.isPending()
    );

    protected readonly estadoStreams = computed(() =>
        this.estadoStreamsQuery.data() ?? null
    );

    protected readonly estadoStreamsCargando = computed(() =>
        this.estadoStreamsQuery.isPending()
    );

    protected readonly ranking = computed<TopProducto[]>(() =>
        this.rankingQuery.data() ?? []
    );

    protected readonly rankingCargando = computed(() =>
        this.rankingQuery.isPending()
    );

    protected readonly productoMasVendido = computed<TopProducto | null>(() =>
    {
        const productos = this.ranking();

        if (productos.length === 0)
            return null;

        return productos.reduce((mayor, actual) =>
            actual.unidades > mayor.unidades ? actual : mayor
        );
    });

    protected readonly productoMayorIngreso = computed<TopProducto | null>(() =>
    {
        const productos = this.ranking();

        if (productos.length === 0)
            return null;

        return productos.reduce((mayor, actual) =>
            actual.ingresos > mayor.ingresos ? actual : mayor
        );
    });

    protected readonly ultimos = computed<EventoReciente[]>(() =>
        this.ultimosQuery.data() ?? []
    );

    protected readonly ultimosCargando = computed(() =>
        this.ultimosQuery.isPending()
    );

    protected readonly overview = computed(() =>
        this.overviewQuery.data()
    );

    protected seleccionarMes(clave: string): void
    {
        this.mesSeleccionado.set(clave);
        this.vista.set('mensual');
    }

    protected volverAnual(): void
    {
        this.vista.set('anual');
        this.mesSeleccionado.set(null);
    }

    protected alturaPx(total: number, maximo: number): number
    {
        return Math.max(
            total > 0 ? 4 : 2,
            Math.round((total / maximo) * ALTO_BARRAS_PX)
        );
    }

    protected colorRoutingKey(routingKey: string | null): string
    {
        switch (routingKey)
        {
            case RK_CREADOS:
                return 'bg-blue-50 text-blue-700';

            case RK_ACEPTADOS:
                return 'bg-emerald-50 text-emerald-700';

            case RK_RESERVADOS:
                return 'bg-teal-50 text-teal-700';

            case RK_FALLIDOS_STOCK:
                return 'bg-red-50 text-red-700';

            case RK_CANCELADOS:
                return 'bg-amber-50 text-amber-700';

            case RK_DLQ:
                return 'bg-rose-50 text-rose-700';

            default:
                return 'bg-[#eef2f7] text-[#4b5563]';
        }
    }

    protected idCorto(id: string | null): string
    {
        if (id === null)
            return '—';

        return id.length > 8 ? `${id.slice(0, 8)}…` : id;
    }

    private pedidosDeMes(clave: string): number
    {
        return this.puntos()
            .filter((punto) =>
                punto.fecha.startsWith(clave) &&
                punto.routingKey === RK_CREADOS
            )
            .reduce((acc, punto) => acc + punto.total, 0);
    }

    private totalesMes(clave: string): Map<string, number>
    {
        const mapa = new Map<string, number>();

        for (const punto of this.puntos())
        {
            if (punto.fecha.startsWith(clave))
            {
                mapa.set(
                    punto.routingKey,
                    (mapa.get(punto.routingKey) ?? 0) + punto.total
                );
            }
        }

        return mapa;
    }

    private kpi(
        etiqueta: string,
        valor: number,
        anterior: number,
        detalle: string
    ): KpiMes
    {
        const delta = anterior > 0
            ? ((valor - anterior) / anterior) * 100
            : null;

        return {
            etiqueta,
            valor: valor.toLocaleString('es-CL'),
            delta,
            detalle,
        };
    }
}