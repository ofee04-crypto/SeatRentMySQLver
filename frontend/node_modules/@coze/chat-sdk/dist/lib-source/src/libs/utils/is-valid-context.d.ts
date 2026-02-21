import { type NonNullableType } from "../types";
export declare const isValidContext: <T extends object>(context: T) => context is NonNullableType<T>;
