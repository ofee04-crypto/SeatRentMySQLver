import { getRegResult } from '../util/ast';
import { traversalLastNode } from './traversal-last-node';
const removePatterns = [/!$/];
const fixPatterns = [
    /!\[(?<text>[^\]\n]*)$/,
    /!\[(?<text>[^\]\n]+)\]$/,
    /!\[(?<text>[^\]\n]+)\]\([^\)\n]*$/,
    /!\[(?<text>[^\]\n]+)\]\([^\)\n]*$/,
];
export function autoFixImg(root) {
    traversalLastNode(root, undefined, (node) => {
        var _a;
        if (!node) {
            return [];
        }
        if (node.type === 'text') {
            const nodesReturn = [];
            const removeMatch = getRegResult(node.value, removePatterns);
            const fixMatch = getRegResult(node.value, fixPatterns);
            const matchResult = fixMatch || removeMatch;
            if (matchResult) {
                nodesReturn.push({
                    type: 'text',
                    value: node.value.slice(0, matchResult.index),
                });
                if (fixMatch) {
                    nodesReturn.push({
                        type: 'image',
                        url: '',
                        alt: ((_a = fixMatch.groups) === null || _a === void 0 ? void 0 : _a.text) || 'image',
                    });
                }
                return nodesReturn;
            }
        }
        return [node];
    });
}
//# sourceMappingURL=autofix-img.js.map