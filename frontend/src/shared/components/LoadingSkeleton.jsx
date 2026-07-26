import React from 'react';

/**
 * LoadingSkeleton renders pulse placeholders while data is being fetched.
 */
export const LoadingSkeleton = ({ count = 3, type = 'card' }) => {
  return (
    <div className="space-y-4 w-full animate-pulse">
      {Array.from({ length: count }).map((_, idx) => (
        <div
          key={idx}
          className="rounded-3xl bg-white p-6 border border-stone-200 shadow-2xs space-y-3"
        >
          <div className="flex items-center justify-between">
            <div className="h-6 w-1/3 bg-stone-200 rounded-xl" />
            <div className="h-5 w-20 bg-stone-150 rounded-full" />
          </div>
          <div className="h-4 w-2/3 bg-stone-150 rounded-lg" />
          <div className="pt-2 flex items-center space-x-3">
            <div className="h-10 flex-1 bg-stone-100 rounded-2xl" />
            <div className="h-10 w-24 bg-stone-150 rounded-2xl" />
          </div>
        </div>
      ))}
    </div>
  );
};

export default LoadingSkeleton;
