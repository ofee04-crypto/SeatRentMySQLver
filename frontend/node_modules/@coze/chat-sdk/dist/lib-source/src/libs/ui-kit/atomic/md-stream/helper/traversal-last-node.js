import { isParent } from '../util/ast';
export const traversalLastNode = (node, parent, handle) => {
    var _a;
    if (isParent(node)) {
        const contents = traversalLastNode(node === null || node === void 0 ? void 0 : node.children[((_a = node === null || node === void 0 ? void 0 : node.children) === null || _a === void 0 ? void 0 : _a.length) - 1], node, handle);
        // @ts-expect-error -- linter-disable-autofix
        node.children.splice(-1, 1, ...contents);
        if (node.children.length === 0) {
            return [];
        }
        return [node];
    }
    else {
        return handle(node, parent);
    }
};
export const getLastNode = (node) => {
    let lastNode = undefined;
    traversalLastNode(node, undefined, (content, parent) => {
        if (!content) {
            // children 会存在未空的情况
            return [];
        }
        if ((parent === null || parent === void 0 ? void 0 : parent.type) === 'link') {
            lastNode = parent;
        }
        else {
            lastNode = content;
        }
        return [content];
    });
    return lastNode;
};
export const isConsumeTextToShow = (node) => {
    const lastNode = getLastNode(node);
    if ((lastNode === null || lastNode === void 0 ? void 0 : lastNode.type) === 'image' || (lastNode === null || lastNode === void 0 ? void 0 : lastNode.type) === 'link') {
        return false;
    }
    return true;
};
//# sourceMappingURL=traversal-last-node.js.map