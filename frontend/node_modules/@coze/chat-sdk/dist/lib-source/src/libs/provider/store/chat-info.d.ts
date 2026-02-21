import type { ChatInfoStore, ChatInfo } from "../../types";
declare const createChatInfoStore: ({ chat }: {
    chat: ChatInfo;
}) => import("zustand").UseBoundStore<import("zustand").StoreApi<ChatInfoStore>>;
export type CreateChatInfoStore = ReturnType<typeof createChatInfoStore>;
export declare const useCreateChatInfoStore: () => import("zustand").UseBoundStore<import("zustand").StoreApi<ChatInfoStore>>;
export {};
