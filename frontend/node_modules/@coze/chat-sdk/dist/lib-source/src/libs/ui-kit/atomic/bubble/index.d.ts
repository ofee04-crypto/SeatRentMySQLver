import React from 'react';
export interface BubbleProps {
    className?: string;
    children?: React.ReactNode;
    isActive?: boolean;
    isNeedBorder?: boolean;
    canClick?: boolean;
    onClick?: () => void;
}
export declare const Bubble: ({ className, children, isNeedBorder, isActive, canClick, onClick, }: BubbleProps) => import("react/jsx-runtime").JSX.Element;
