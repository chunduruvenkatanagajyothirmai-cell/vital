import React from "react";

export default function VitalLogo({
  variant = "full", // "full" | "header" | "badge" | "icon"
  size = "md", // "sm" | "md" | "lg"
  className = "",
  showTagline = true
}) {
  const iconDimensions = {
    sm: "w-9 h-9",
    md: "w-12 h-12",
    lg: "w-20 h-20"
  }[size] || "w-12 h-12";

  // Pure SVG reproduction of official Vital branding icon
  const renderSvgIcon = () => (
    <div className={`relative ${iconDimensions} flex-shrink-0 select-none`}>
      <svg
        viewBox="0 0 100 100"
        className="w-full h-full drop-shadow-sm"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <defs>
          {/* Soft light-blue gradient border & card fill */}
          <linearGradient id="vitalBgBorder" x1="0" y1="0" x2="100" y2="100" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stopColor="#BAE6FD" />
            <stop offset="100%" stopColor="#7DD3FC" />
          </linearGradient>
          <linearGradient id="vitalCardFill" x1="0" y1="0" x2="0" y2="100" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stopColor="#FFFFFF" />
            <stop offset="100%" stopColor="#F0F9FF" />
          </linearGradient>

          {/* Supportive blue-to-teal cupping hand gradient */}
          <linearGradient id="handGradient" x1="0" y1="0" x2="100" y2="100" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stopColor="#0284C7" />
            <stop offset="60%" stopColor="#0EA5E9" />
            <stop offset="100%" stopColor="#14B8A6" />
          </linearGradient>

          {/* Solid blue medical cross gradient */}
          <linearGradient id="crossGradient" x1="0" y1="0" x2="0" y2="100" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stopColor="#1D4ED8" />
            <stop offset="100%" stopColor="#1E40AF" />
          </linearGradient>

          {/* Red heart glow */}
          <linearGradient id="heartGradient" x1="0" y1="0" x2="0" y2="100" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stopColor="#EF4444" />
            <stop offset="100%" stopColor="#DC2626" />
          </linearGradient>
        </defs>

        {/* 1. Rounded-Square App Icon Background with soft light-blue gradient border */}
        <rect
          x="3"
          y="3"
          width="94"
          height="94"
          rx="22"
          fill="url(#vitalCardFill)"
          stroke="url(#vitalBgBorder)"
          strokeWidth="3.5"
        />

        {/* 2. Solid Blue Medical Cross at Center */}
        {/* Vertical beam */}
        <rect x="41" y="16" width="18" height="52" rx="4" fill="url(#crossGradient)" />
        {/* Horizontal beam */}
        <rect x="24" y="32" width="52" height="18" rx="4" fill="url(#crossGradient)" />

        {/* 3. Stylized Human Figure (White with protective cape/collar) */}
        {/* Head */}
        <circle cx="50" cy="27" r="6.5" fill="#FFFFFF" />
        {/* Protective open collar / torso cape */}
        <path
          d="M38 48 C38 37 43 36 50 36 C57 36 62 37 62 48 C62 55 58 60 50 60 C42 60 38 55 38 48 Z"
          fill="#FFFFFF"
        />

        {/* 4. Red Heart with White ECG Pulse Line on chest */}
        <path
          d="M50 43.5 
             C48.5 40 43.5 39.5 42 42.5 
             C40.5 45.5 43 49 50 53.5 
             C57 49 59.5 45.5 58 42.5 
             C56.5 39.5 51.5 40 50 43.5 Z"
          fill="url(#heartGradient)"
        />
        {/* White ECG heartbeat pulse line inside heart */}
        <path
          d="M44 46.5 L46.5 46.5 L48 43.5 L50 49 L51.5 45 L53 47.5 L56 46.5"
          stroke="#FFFFFF"
          strokeWidth="1.2"
          strokeLinecap="round"
          strokeLinejoin="round"
        />

        {/* 5. Supportive, stylized blue-to-teal cupping hand wrapping underneath & left */}
        <path
          d="M21 34 
             C24 45 22 58 30 67 
             C37 75 49 76 60 75 
             C69 74 77 69 81 64 
             C74 67 65 69 57 69 
             C46 69 36 65 31 57 
             C27 50 26 42 27 34 
             C25 33 22 33 21 34 Z"
          fill="url(#handGradient)"
        />
        {/* Hand contour swoosh */}
        <path
          d="M33 70 
             C41 77 55 77 66 73 
             C72 71 78 67 82 62 
             C77 64 71 66 64 67 
             C52 69 41 68 33 70 Z"
          fill="#0EA5E9"
          opacity="0.85"
        />
      </svg>
    </div>
  );

  // Icon only
  if (variant === "icon") {
    return renderSvgIcon();
  }

  // Header variant (compact horizontal alignment)
  if (variant === "header") {
    return (
      <div className={`flex items-center gap-3 ${className}`}>
        {renderSvgIcon()}
        <div className="flex flex-col">
          <div className="flex items-center gap-2">
            <span className="font-extrabold tracking-wider text-base text-[#0F2847]">
              VITAL
            </span>
            <span className="bg-[#EEF2EF] text-[#2F8F5B] text-[10px] font-bold px-1.5 py-0.5 rounded border border-[#DCE4E0]">
              VERIFIED
            </span>
          </div>

          {/* Thin blue dividing line with central pulse symbol */}
          <div className="flex items-center my-0.5 w-full max-w-[210px]">
            <div className="flex-1 h-[1.5px] bg-[#3B82F6]/30"></div>
            <svg className="w-4 h-2 text-[#2563EB] mx-1" viewBox="0 0 20 10" fill="none">
              <path d="M0 5 H6 L8 1 L11 9 L13 3 L15 6 H20" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
            <div className="flex-1 h-[1.5px] bg-[#3B82F6]/30"></div>
          </div>

          {showTagline && (
            <p className="text-[11px] text-[#475569] font-medium leading-none">
              Not just an alert. A confirmed response.
            </p>
          )}
        </div>
      </div>
    );
  }

  // Full Hero / Brand Banner layout (Centered prominent showcase)
  return (
    <div className={`flex flex-col items-center text-center p-5 bg-white border border-[#DCE4E0] rounded-2xl shadow-sm ${className}`}>
      {/* Official App Logo Icon */}
      <div className="mb-3">
        {renderSvgIcon()}
      </div>

      {/* Brand Name */}
      <h1 className="text-2xl font-black tracking-widest text-[#0A2540]">
        VITAL
      </h1>

      {/* Thin blue dividing line with a small central ECG heartbeat pulse wave symbol */}
      <div className="flex items-center justify-center my-2 w-48 max-w-full">
        <div className="flex-1 h-[1.5px] bg-[#2563EB]/40"></div>
        <svg className="w-6 h-3 text-[#1D4ED8] mx-1.5 flex-shrink-0" viewBox="0 0 24 12" fill="none">
          <path
            d="M1 6 H7 L9.5 1 L13.5 11 L16 3.5 L18 7.5 H23"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
        <div className="flex-1 h-[1.5px] bg-[#2563EB]/40"></div>
      </div>

      {/* Official Tagline */}
      <p className="text-xs sm:text-sm text-[#334155] font-semibold italic">
        "Not just an alert. A confirmed response."
      </p>
    </div>
  );
}
