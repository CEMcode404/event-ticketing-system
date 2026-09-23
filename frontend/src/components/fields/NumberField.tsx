interface NumberFieldProps {
  label: string;
  value: string;
  onChange: (value: string) => void;
  required?: boolean;
  min?: string;
  step?: string;
  className?: string;
}

export function NumberField({ label, value, onChange, required, min, step, className = "" }: NumberFieldProps) {
  return (
    <label className={`block text-sm text-muted ${className}`}>
      {label}
      <input
        type="number"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        required={required}
        min={min}
        step={step}
        className="mt-1.5 w-full rounded border border-border bg-bg px-3 py-2 text-ink outline-none focus:border-accent"
      />
    </label>
  );
}