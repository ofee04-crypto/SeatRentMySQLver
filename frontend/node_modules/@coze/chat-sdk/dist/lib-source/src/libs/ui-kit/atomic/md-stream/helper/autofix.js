import { autoFixLink } from './autofix-link';
import { autoFixImg } from './autofix-img';
export const autoFix = (root) => {
    autoFixImg(root);
    autoFixLink(root);
    //  autoFixIndicator(root);
};
//# sourceMappingURL=autofix.js.map