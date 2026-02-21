import type { ChatStatusStore } from "../../types";
declare const createChatStatusStore: ({ isReadonly }: {
    isReadonly?: boolean | undefined;
}) => import("zustand").UseBoundStore<import("zustand").StoreApi<ChatStatusStore>>;
export type CreateChatStatusStore = ReturnType<typeof createChatStatusStore>;
export declare const useCreateStatusStore: () => import("zustand").UseBoundStore<import("zustand").StoreApi<ChatStatusStore>>;
export {};
