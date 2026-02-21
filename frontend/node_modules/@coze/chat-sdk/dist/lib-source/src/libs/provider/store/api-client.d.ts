import type { ApiCreateStoreProps, ApiClientStore } from "../../types";
export declare const createApiClientStore: ({ auth, setting, chat, }: ApiCreateStoreProps) => import("zustand").UseBoundStore<import("zustand").StoreApi<ApiClientStore>>;
export type CreateApiClientStore = ReturnType<typeof createApiClientStore>;
