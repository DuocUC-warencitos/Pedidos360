import { environment } from "@env/environment";

const base = `${environment.apiGatewayUrl}/api/v1/productos`;

export const apiProductos = 
{
    get:
    {
        all: base,
        byId: (id: string) => `${base}/${id}`
    },
    post:
    {
        save: base
    },
    put:
    {
        updatById: (id: string) => `${base}/${id}`
    },
    delete:
    {
        deleteById: (id: string) => `${base}/${id}`
    }
}