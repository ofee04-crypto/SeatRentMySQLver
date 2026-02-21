import { jsx as _jsx } from "react/jsx-runtime";
import { View } from '@tarojs/components';
import { Phrase } from '../../phrase';
import styles from './index.module.less';
export const Heading = ({ node }) => {
    const depth = node.depth > 3 ? 3 : node.depth;
    return (_jsx(View, Object.assign({ className: styles[`heading-${depth}`] }, { children: node.children.map((item, index) => (_jsx(Phrase, { node: item }, index))) })));
};
//# sourceMappingURL=index.js.map