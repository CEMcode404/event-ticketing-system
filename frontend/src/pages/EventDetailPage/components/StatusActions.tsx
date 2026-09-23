import { Button } from "../../../components/Button";
import type { EventStatus } from "../../../types";

interface StatusActionsProps {
  status: EventStatus;
  updating: boolean;
  onChangeStatus: (status: EventStatus) => void;
  onEdit: () => void;
}

export function StatusActions({ status, updating, onChangeStatus, onEdit }: StatusActionsProps) {
  return (
    <div className="flex gap-3">
      {status !== "PUBLISHED" && status !== "CANCELLED" && (
        <Button onClick={() => onChangeStatus("PUBLISHED")} disabled={updating}>
          Publish
        </Button>
      )}
      {status === "PUBLISHED" && (
        <Button variant="secondary" onClick={() => onChangeStatus("SUSPENDED")} disabled={updating}>
          Suspend
        </Button>
      )}
      {status !== "CANCELLED" && (
        <Button variant="danger" onClick={() => onChangeStatus("CANCELLED")} disabled={updating}>
          Cancel event
        </Button>
      )}
      <Button variant="secondary" onClick={onEdit}>
        Edit details
      </Button>
    </div>
  );
}