import type { UserInfoStore, UserInfo } from "../../types";
declare const createUserInfoStore: ({ user }: {
    user: UserInfo;
}) => import("zustand").UseBoundStore<import("zustand").StoreApi<UserInfoStore>>;
export type CreateUserInfoStore = ReturnType<typeof createUserInfoStore>;
export declare const useCreateUserInfoStore: () => import("zustand").UseBoundStore<import("zustand").StoreApi<UserInfoStore>>;
export {};
