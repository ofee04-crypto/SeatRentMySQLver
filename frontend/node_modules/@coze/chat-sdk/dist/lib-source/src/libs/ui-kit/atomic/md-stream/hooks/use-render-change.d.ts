interface RenderChangeProps {
    showMarkdown: string;
    markdown: string;
    isFinish?: boolean;
    onMarkdownEnd?: () => void;
    onRenderMarkdownChange?: (markdown: string) => void;
}
export declare const useRenderChange: ({ showMarkdown, markdown, isFinish, onMarkdownEnd, onRenderMarkdownChange, }: RenderChangeProps) => void;
export {};
