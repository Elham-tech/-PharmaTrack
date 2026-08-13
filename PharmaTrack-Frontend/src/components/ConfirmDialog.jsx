/**
 * ConfirmDialog Component
 * Modal confirmation dialog for destructive actions like delete.
 */
export default function ConfirmDialog({ open, title, message, onConfirm, onCancel, confirmLabel }) {
  if (!open) return null;

  return (
    <div className="modal-overlay" onClick={onCancel}>
      <div className="modal confirm-dialog" onClick={(e) => e.stopPropagation()}>
        <div className="modal-body">
          <div className="icon-wrapper">⚠️</div>
          <h3>{title || 'Are you sure?'}</h3>
          <p>{message || 'This action cannot be undone.'}</p>
          <div className="actions">
            <button className="btn btn-secondary" onClick={onCancel}>Cancel</button>
            <button className="btn btn-danger" onClick={onConfirm}>{confirmLabel || 'Delete'}</button>
          </div>
        </div>
      </div>
    </div>
  );
}
