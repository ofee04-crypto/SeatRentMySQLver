import { jsx as _jsx } from "react/jsx-runtime";
import cls from 'classnames';
import { View } from '@tarojs/components';
import styles from './index.module.less';
export const Bubble = ({ className, children, isNeedBorder = true, isActive = false, canClick = false, onClick, }) => (_jsx(View, Object.assign({ className: cls(styles.bubble, className, {
        [styles.active]: isActive,
        [styles['can-click']]: canClick,
        [styles['is-need-border']]: isNeedBorder,
    }), onClick: onClick }, { children: children })));
//# sourceMappingURL=index.js.map