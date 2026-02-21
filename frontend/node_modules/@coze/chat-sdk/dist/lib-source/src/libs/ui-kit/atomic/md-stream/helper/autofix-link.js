import { getRegResult } from '../util/ast';
import { traversalLastNode } from './traversal-last-node';
const fixPatterns = [
    /\[(?<text>[^\]\n]+)$/,
    /\[(?<text>[^\]\n]+)\]$/,
    /\[(?<text>[^\]\n]+)\]\([^\)\n]*$/,
];
const removePatterns = [/\[$/];
export function autoFixLink(root) {
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
                        type: 'link',
                        title: null,
                        url: '#',
                        children: [
                            {
                                type: 'text',
                                value: ((_a = fixMatch.groups) === null || _a === void 0 ? void 0 : _a.text) || '',
                            },
                        ],
                    });
                }
                return nodesReturn;
            }
        }
        return [node];
    });
}
//# sourceMappingURL=autofix-link.js.map