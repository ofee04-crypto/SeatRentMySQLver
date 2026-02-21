import { Language } from "../../types";
import SpecialCn from './special/zh';
import SpecialEn from './special/en';
import SimpleCn from './simple/zh';
import SimpleEn from './simple/en';
export const resource = {
    [Language.EN]: {
        simple: SimpleEn,
        special: SpecialEn,
    },
    [Language.ZH_CN]: {
        simple: SimpleCn,
        special: SpecialCn,
    },
};
//# sourceMappingURL=index.js.map