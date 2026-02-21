import type { ChatFrameworkProps, ChatPropsStore } from "../../types";
declare const createChatPropsStore: (props: ChatFrameworkProps) => import("zustand").UseBoundStore<import("zustand").StoreApi<ChatPropsStore>>;
export type CreateChatPropsStore = ReturnType<typeof createChatPropsStore>;
export declare const useCreateChatPropsStore: () => import("zustand").UseBoundStore<import("zustand").StoreApi<ChatPropsStore>>;
export {};
