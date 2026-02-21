import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import cls from 'classnames';
import { View, Text } from '@tarojs/components';
import { SvgLoading } from '../svg';
import styles from './index.module.less';
export const Spinning = ({ className, text, textClassName, svgClassName, size }) => (_jsxs(View, Object.assign({ className: cls(styles.container, className, {
        [styles[size || 'medium']]: true,
    }) }, { children: [_jsx(SvgLoading, { className: cls(styles.svg, svgClassName) }), text ? (_jsx(Text, Object.assign({ className: cls(styles.text, textClassName) }, { children: text }))) : null] })));
//# sourceMappingURL=index.js.map