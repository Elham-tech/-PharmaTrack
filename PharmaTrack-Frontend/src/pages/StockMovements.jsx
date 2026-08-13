/**
 * Stock Movements Page
 * Tracks all stock-in and stock-out operations with batch and medicine associations.
 */
import { useState, useEffect } from 'react';
import { stockMovementApi, medicineApi, inventoryBatchApi } from '../api/api';
import { useToast } from '../components/Toast';

const MOVEMENT_TYPES = ['STOCK_IN', 'STOCK_OUT', 'ADJUSTMENT', 'RETURN', 'EXPIRED_REMOVAL'];
const TYPE_COLORS = { STOCK_IN: 'green', STOCK_OUT: 'red', ADJUSTMENT: 'amber', RETURN: 'indigo', EXPIRED_REMOVAL: 'gray' };

export default function StockMovements() {
  const toast = useToast();
  const [movements, setMovements] = useState([]);
  const [medicines, setMedicines] = useState([]);
  const [batches, setBatches] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [operation, setOperation] = useState(''); // 'stock-in' or 'stock-out'
  const [form, setForm] = useState({ medicine: '', inventoryBatch: '', quantity: '', notes: '', referenceNumber: '' });
  const [errors, setErrors] = useState({});

  const loadData = async () => {
    try {
      setLoading(true);
      const mov = await stockMovementApi.getAll().catch(e => { toast.error('Failed to load movements'); return []; });
      const med = await medicineApi.getAll().catch(e => { toast.error('Failed to load medicines'); return []; });
      const bat = await inventoryBatchApi.getAll().catch(e => { toast.error('Failed to load batches'); return []; });
      setMovements(mov); setMedicines(med); setBatches(bat);
    } finally { setLoading(false); }
  };

  useEffect(() => { loadData(); }, []);

  const filtered = movements.filter(m => {
    const matchSearch = !search ||
      m.medicine?.name?.toLowerCase().includes(search.toLowerCase()) ||
      m.referenceNumber?.toLowerCase().includes(search.toLowerCase());
    const matchType = !typeFilter || m.movementType === typeFilter;
    return matchSearch && matchType;
  });

  const openModal = (op) => {
    setOperation(op);
    setForm({ medicine: '', inventoryBatch: '', quantity: '', notes: '', referenceNumber: '' });
    setErrors({});
    setModalOpen(true);
  };

  const validate = () => {
    const e = {};
    if (!form.medicine) e.medicine = 'Medicine is required';
    if (!form.inventoryBatch) e.inventoryBatch = 'Batch is required';
    if (!form.quantity || parseInt(form.quantity) <= 0) e.quantity = 'Quantity must be > 0';
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleProcess = async () => {
    if (!validate()) return;
    const payload = {
      medicineId: parseInt(form.medicine),
      inventoryBatchId: parseInt(form.inventoryBatch),
      quantity: parseInt(form.quantity),
      notes: form.notes,
      referenceNumber: form.referenceNumber,
    };
    try {
      if (operation === 'stock-in') {
        await stockMovementApi.processStockIn(payload);
        toast.success('Stock-in processed');
      } else {
        await stockMovementApi.processStockOut(payload);
        toast.success('Stock-out processed');
      }
      setModalOpen(false);
      loadData();
    } catch (err) { toast.error(err.message); }
  };

  return (
    <>
      <div className="page-header">
        <div><h1>Stock Movements</h1><div className="page-header-subtitle">Track all inventory movements</div></div>
        <div style={{ display: 'flex', gap: 8 }}>
          <button className="btn btn-primary" onClick={() => openModal('stock-in')} style={{ background: 'var(--success)' }}>📥 Stock In</button>
          <button className="btn btn-primary" onClick={() => openModal('stock-out')} style={{ background: 'var(--danger)' }}>📤 Stock Out</button>
        </div>
      </div>
      <div className="page-body">
        <div className="toolbar">
          <div className="search-box">
            <span className="search-icon">🔍</span>
            <input placeholder="Search movements..." value={search} onChange={(e) => setSearch(e.target.value)} />
          </div>
          <select className="form-select" style={{ width: 180 }} value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)}>
            <option value="">All Types</option>
            {MOVEMENT_TYPES.map(t => <option key={t} value={t}>{t.replace(/_/g, ' ')}</option>)}
          </select>
        </div>
        <div className="card">
          <div className="table-wrapper">
            <table>
              <thead><tr><th>ID</th><th>Type</th><th>Medicine</th><th>Batch</th><th>Qty</th><th>Reference</th><th>By</th><th>Date</th></tr></thead>
              <tbody>
                {loading ? (
                  <tr><td colSpan="8"><div className="loading-spinner"><div className="spinner" /></div></td></tr>
                ) : filtered.length === 0 ? (
                  <tr><td colSpan="8"><div className="empty-state"><div className="icon">🔄</div><h3>No movements found</h3></div></td></tr>
                ) : filtered.map(m => (
                  <tr key={m.id}>
                    <td>{m.id}</td>
                    <td><span className={`badge badge-${TYPE_COLORS[m.movementType] || 'gray'}`}>{m.movementType?.replace(/_/g, ' ')}</span></td>
                    <td>{m.medicine?.name || '-'}</td>
                    <td>{m.inventoryBatch?.batchNumber || '-'}</td>
                    <td>{m.quantity}</td>
                    <td>{m.referenceNumber || '-'}</td>
                    <td>{m.performedBy?.fullName || '-'}</td>
                    <td>{m.movementDate ? new Date(m.movementDate).toLocaleString() : '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {modalOpen && (
        <div className="modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{operation === 'stock-in' ? '📥 Process Stock In' : '📤 Process Stock Out'}</h2>
              <button className="modal-close" onClick={() => setModalOpen(false)}>×</button>
            </div>
            <div className="modal-body">
              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">Medicine <span className="required">*</span></label>
                  <select className={`form-select ${errors.medicine ? 'error' : ''}`} value={form.medicine} onChange={e => setForm({...form, medicine: e.target.value})}>
                    <option value="">Select medicine</option>
                    {medicines.map(m => <option key={m.id} value={m.id}>{m.name}</option>)}
                  </select>
                  {errors.medicine && <div className="form-error">{errors.medicine}</div>}
                </div>
                <div className="form-group">
                  <label className="form-label">Batch <span className="required">*</span></label>
                  <select className={`form-select ${errors.inventoryBatch ? 'error' : ''}`} value={form.inventoryBatch} onChange={e => setForm({...form, inventoryBatch: e.target.value})}>
                    <option value="">Select batch</option>
                    {batches.map(b => <option key={b.id} value={b.id}>{b.batchNumber} (Rem: {b.quantityRemaining})</option>)}
                  </select>
                  {errors.inventoryBatch && <div className="form-error">{errors.inventoryBatch}</div>}
                </div>
              </div>
              <div className="form-group">
                <label className="form-label">Quantity <span className="required">*</span></label>
                <input className={`form-input ${errors.quantity ? 'error' : ''}`} type="number" min="1" value={form.quantity} onChange={e => setForm({...form, quantity: e.target.value})} />
                {errors.quantity && <div className="form-error">{errors.quantity}</div>}
                <div className="form-hint" style={{ fontSize: 12, color: 'var(--text-secondary)' }}>
                  The movement will be attributed to you (the logged-in user).
                </div>
              </div>
              <div className="form-group">
                <label className="form-label">Reference Number</label>
                <input className="form-input" value={form.referenceNumber} onChange={e => setForm({...form, referenceNumber: e.target.value})} />
              </div>
              <div className="form-group">
                <label className="form-label">Notes</label>
                <textarea className="form-textarea" value={form.notes} onChange={e => setForm({...form, notes: e.target.value})} />
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn btn-secondary" onClick={() => setModalOpen(false)}>Cancel</button>
              <button className="btn btn-primary" onClick={handleProcess}>
                {operation === 'stock-in' ? '📥 Process Stock In' : '📤 Process Stock Out'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
