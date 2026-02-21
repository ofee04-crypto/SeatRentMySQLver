import { useEffect } from 'react';
export const useRenderChange = ({ showMarkdown, markdown, isFinish, onMarkdownEnd, onRenderMarkdownChange, }) => {
    useEffect(() => {
        if (showMarkdown === markdown && isFinish) {
            onMarkdownEnd === null || onMarkdownEnd === void 0 ? void 0 : onMarkdownEnd();
        }
    }, [showMarkdown, markdown, isFinish]);
    useEffect(() => {
        onRenderMarkdownChange === null || onRenderMarkdownChange === void 0 ? void 0 : onRenderMarkdownChange(showMarkdown);
    }, [showMarkdown]);
};
//# sourceMappingURL=use-render-change.js.map