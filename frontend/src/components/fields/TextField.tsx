interface TextFieldProps {
  label: string;
  value: string;
  onChange: (value: string) => void;
  required?: boolean;
  placeholder?: string;
  className?: string;
}

export function TextField({ label, value, onChange, required, placeholder, className = "" }: TextFieldProps) {
  return (
    <label className={`block text-sm text-muted ${className}`}>
      {label}
      <input
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        required={required}
        placeholder={placeholder}
        className="mt-1.5 w-full rounded border border-border bg-bg px-3 py-2 text-ink outline-none focus:border-accent"
      />
    </label>
  );
}