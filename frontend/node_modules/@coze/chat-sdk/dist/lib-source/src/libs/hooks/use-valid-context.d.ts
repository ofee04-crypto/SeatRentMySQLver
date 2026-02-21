import { type Context } from 'react';
import { type NullableType } from "../types";
export declare const useValidContext: <T>(context: Context<NullableType<T>>, errorMsg?: string) => import("../types").NonNullableType<NullableType<T>>;
