/**
 * Dispensing Records Page
 * View dispensing records with cashier payment approval workflow:
 * approve (payment processed) or void (not paid) pending records.
 */
import { useState, useEffect } from 'react';
import { dispensingRecordApi } from '../api/api';
import { useToast } from '../components/Toast';
import ConfirmDialog from '../components/ConfirmDialog';

const STATUS_COLORS = { PENDING: 'amber', PAID: 'green', VOIDED: 'red' };

export default function DispensingRecords() {
  const toast = useToast();
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [confirmAction, setConfirmAction] = useState(null); // { record, action: 'approve' | 'void' }

  const loadData = async () => {
    try {
      setLoading(true);
      const recs = await dispensingRecordApi.getAll().catch(e => { toast.error('Failed to load dispensing records'); return []; });
      setRecords(recs);
    } finally { setLoading(false); }
  };

  useEffect(() => { loadData(); }, []);

  const filtered = records.filter(r => {
    const matchSearch = !search ||
      r.dispensingNumber?.toLowerCase().includes(search.toLowerCase()) ||
      r.medicine?.name?.toLowerCase().includes(search.toLowerCase()) ||
      r.prescription?.patientName?.toLowerCase().includes(search.toLowerCase());
    const matchStatus = !statusFilter || r.paymentStatus === statusFilter;
    return matchSearch && matchStatus;
  });

  const handleApprove = async () => {
    try {
      await dispensingRecordApi.approve(confirmAction.record.id);
      toast.success('Payment approved');
      setConfirmAction(null);
      loadData();
    } catch (err) { toast.error(err.message); }
  };

  const handleVoid = async () => {
    try {
      await dispensingRecordApi.voidRecord(confirmAction.record.id);
      toast.success('Dispensing voided — medicine returned to stock');
      setConfirmAction(null);
      loadData();
    } catch (err) { toast.error(err.message); }
  };

  return (
    <>
      <div className="page-header">
        <div><h1>Dispensing Records</h1><div className="page-header-subtitle">View transactions and process payments</div></div>
      </div>
      <div className="page-body">
        <div className="toolbar">
          <div className="search-box">
            <span className="search-icon">🔍</span>
            <input placeholder="Search by number, medicine, or patient..." value={search} onChange={(e) => setSearch(e.target.value)} />
          </div>
          <select className="form-select" style={{ width: 160 }} value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            <option value="">All Statuses</option>
            <option value="PENDING">Pending</option>
            <option value="PAID">Paid</option>
            <option value="VOIDED">Voided</option>
          </select>
        </div>
        <div className="card">
          <div className="table-wrapper">
            <table>
              <thead><tr><th>Dispensing #</th><th>Patient</th><th>Medicine</th><th>Batch</th><th>Qty</th><th>Unit Price</th><th>Total</th><th>Dispensed By</th><th>Payment</th><th>Date</th><th>Actions</th></tr></thead>
              <tbody>
                {loading ? (
                  <tr><td colSpan="11"><div className="loading-spinner"><div className="spinner" /></div></td></tr>
                ) : filtered.length === 0 ? (
                  <tr><td colSpan="11"><div className="empty-state"><div className="icon">🧾</div><h3>No dispensing records found</h3></div></td></tr>
                ) : filtered.map(r => (
                  <tr key={r.id}>
                    <td><strong>{r.dispensingNumber}</strong></td>
                    <td>{r.prescription?.patientName || '-'}</td>
                    <td>{r.medicine?.name || '-'}</td>
                    <td>{r.inventoryBatch?.batchNumber || '-'}</td>
                    <td>{r.quantityDispensed}</td>
                    <td>${parseFloat(r.unitPrice).toFixed(2)}</td>
                    <td><strong>${parseFloat(r.totalPrice).toFixed(2)}</strong></td>
                    <td>{r.dispensedBy?.fullName || '-'}</td>
                    <td>
                      <span className={`badge badge-${STATUS_COLORS[r.paymentStatus] || 'gray'}`}>{r.paymentStatus || 'PENDING'}</span>
                      {r.processedBy && <div style={{ fontSize: 11, color: 'var(--text-secondary)', marginTop: 2 }}>by {r.processedBy.fullName}</div>}
                    </td>
                    <td>{r.dispensingDate ? new Date(r.dispensingDate).toLocaleString() : '-'}</td>
                    <td>
                      {r.paymentStatus === 'PENDING' ? (
                        <div style={{ display: 'flex', gap: 4 }}>
                          <button className="btn btn-ghost btn-sm" style={{ color: 'var(--success)' }} onClick={() => setConfirmAction({ record: r, action: 'approve' })}>✓ Approve</button>
                          <button className="btn btn-ghost btn-sm" style={{ color: 'var(--danger)' }} onClick={() => setConfirmAction({ record: r, action: 'void' })}>✕ Void</button>
                        </div>
                      ) : (
                        <span style={{ color: 'var(--text-secondary)', fontSize: 12 }}>—</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <ConfirmDialog
        open={!!confirmAction}
        title={confirmAction?.action === 'approve' ? 'Approve Payment' : 'Void Dispensing'}
        message={
          confirmAction?.action === 'approve'
            ? `Mark dispensing "${confirmAction?.record?.dispensingNumber}" as payment processed (${confirmAction?.record?.dispensedBy?.fullName || ''})?`
            : `Void dispensing "${confirmAction?.record?.dispensingNumber}"? The medicine (${confirmAction?.record?.quantityDispensed}) will be returned to stock and the prescription will be marked Voided.`
        }
        confirmLabel={confirmAction?.action === 'approve' ? 'Approve' : 'Void'}
        onConfirm={confirmAction?.action === 'approve' ? handleApprove : handleVoid}
        onCancel={() => setConfirmAction(null)}
      />
    </>
  );
}
