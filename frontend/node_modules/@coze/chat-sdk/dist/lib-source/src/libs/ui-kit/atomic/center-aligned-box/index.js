var __rest = (this && this.__rest) || function (s, e) {
    var t = {};
    for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p) && e.indexOf(p) < 0)
        t[p] = s[p];
    if (s != null && typeof Object.getOwnPropertySymbols === "function")
        for (var i = 0, p = Object.getOwnPropertySymbols(s); i < p.length; i++) {
            if (e.indexOf(p[i]) < 0 && Object.prototype.propertyIsEnumerable.call(s, p[i]))
                t[p[i]] = s[p[i]];
        }
    return t;
};
import { jsx as _jsx } from "react/jsx-runtime";
import cls from 'classnames';
import { View } from '@tarojs/components';
import styles from './index.module.less';
export const CenterAlignedBox = (_a) => {
    var { className, width, height, children } = _a, rest = __rest(_a, ["className", "width", "height", "children"]);
    return (_jsx(View, Object.assign({ className: cls(styles.wrapper, className), style: {
            width,
            height,
        } }, rest, { children: children })));
};
//# sourceMappingURL=index.js.map