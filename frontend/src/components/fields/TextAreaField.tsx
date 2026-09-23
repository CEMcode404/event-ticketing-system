interface TextAreaFieldProps {
  label: string;
  value: string;
  onChange: (value: string) => void;
  rows?: number;
  className?: string;
}

export function TextAreaField({ label, value, onChange, rows = 3, className = "" }: TextAreaFieldProps) {
  return (
    <label className={`block text-sm text-muted ${className}`}>
      {label}
      <textarea
        value={value}
        onChange={(e) => onChange(e.target.value)}
        rows={rows}
        className="mt-1.5 w-full resize-none rounded border border-border bg-bg px-3 py-2 text-ink outline-none focus:border-accent"
      />
    </label>
  );
}