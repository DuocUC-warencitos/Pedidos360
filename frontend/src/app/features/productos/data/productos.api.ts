import { environment } from "@env/environment";

const base = `${environment.apiGatewayUrl}/v1/productos`;

export const apiProductos = 
{
    get:
    {
        all: base,
        byId: (id: string) => `${base}/${id}`
    },
    post:
    {
        
    },
    put:
    {
        
    },
    delete:
    {
        
    }
}