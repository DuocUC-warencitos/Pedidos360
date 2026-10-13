import { environment } from "@env/environment";

const base = `${environment.apiGatewayUrl}/v1/admin/productos`;

export const apiAdminProductos =
{
    get:
    {
        findAll: base
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