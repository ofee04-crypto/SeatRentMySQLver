import { II18n } from "../types";
export declare class I18n implements II18n {
    readonly language: any;
    constructor(language: string);
    t(key: string, _options?: Record<string, unknown>, _fallbackText?: string): string;
}
