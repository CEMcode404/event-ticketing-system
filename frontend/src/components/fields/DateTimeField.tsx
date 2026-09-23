interface DateTimeFieldProps {
  label: string;
  value: string;
  onChange: (value: string) => void;
  required?: boolean;
  className?: string;
}

export function DateTimeField({ label, value, onChange, required, className = "" }: DateTimeFieldProps) {
  return (
    <label className={`block text-sm text-muted ${className}`}>
      {label}
      <input
        type="datetime-local"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        required={required}
        className="mt-1.5 w-full rounded border border-border bg-bg px-3 py-2 text-ink outline-none focus:border-accent [color-scheme:dark]"
      />
    </label>
  );
}