import { environment } from "../../../../environments/environment";

const base = `${environment.apiBaseUrl}/api/v1/pedidos`;

export const apiPedidos =
{
    get:
    {
        all: base
    },
    post:
    {
        create: base
    }
};