import { traversalLastNode } from './traversal-last-node';
const indicator = {
    type: 'indicator',
};
export function addIndicator(root) {
    traversalLastNode(root, undefined, (node) => {
        if (!node) {
            return [indicator];
        }
        if ((node === null || node === void 0 ? void 0 : node.type) === 'code') {
            node.children = [
                {
                    type: 'text',
                    value: node.value,
                },
                indicator,
            ];
            return [node];
        }
        return [node, indicator];
    });
}
//# sourceMappingURL=add-indicator.js.map