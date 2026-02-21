interface SmoothShowMarkdownProps {
    markdown: string;
    isSmooth?: boolean;
    isFinish?: boolean;
    interval?: number;
    onMarkdownEnd?: () => void;
    onRenderMarkdownChange?: (md: string) => void;
}
export declare const useSmoothShowMarkdown: ({ isSmooth, interval, markdown, isFinish, }: SmoothShowMarkdownProps) => {
    isShowIndicator: boolean | undefined;
    showMarkdown: string;
    showMoreByte: (count?: number) => void;
};
export {};
