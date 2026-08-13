/**
 * Inventory Batches Page
 * Management for medicine inventory batches with expiry tracking.
 */
import { useState, useEffect } from 'react';
import { inventoryBatchApi, medicineApi, supplierApi } from '../api/api';
import { useToast } from '../components/Toast';
import ConfirmDialog from '../components/ConfirmDialog';

const emptyForm = {
  batchNumber: '', medicine: '', supplier: '',
  unitCost: '', unitPrice: '', manufacturingDate: '', expiryDate: '', expired: false
};

export default function InventoryBatches() {
  const toast = useToast();
  const [batches, setBatches] = useState([]);
  const [medicines, setMedicines] = useState([]);
  const [suppliers, setSuppliers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [errors, setErrors] = useState({});
  const [confirmDelete, setConfirmDelete] = useState(null);

  const loadData = async () => {
    try {
      setLoading(true);
      const b = await inventoryBatchApi.getAll().catch(e => { toast.error('Failed to load batches'); return []; });
      const m = await medicineApi.getAll().catch(e => { toast.error('Failed to load medicines'); return []; });
      const s = await supplierApi.getAll().catch(e => { toast.error('Failed to load suppliers'); return []; });
      setBatches(b); setMedicines(m); setSuppliers(s);
    } finally { setLoading(false); }
  };

  useEffect(() => { loadData(); }, []);

  const filtered = batches.filter(b => b.batchNumber?.toLowerCase().includes(search.toLowerCase()));

  const openCreate = () => { setEditing(null); setForm(emptyForm); setErrors({}); setModalOpen(true); };
  const openEdit = (b) => {
    setEditing(b);
    // Quantity fields are movement-managed, never carried into the form or payload.
    const { quantity, quantityRemaining, ...batchFields } = b;
    setForm({
      ...batchFields,
      medicine: b.medicine?.id || '',
      supplier: b.supplier?.id || '',
      unitCost: b.unitCost || '',
      unitPrice: b.unitPrice || '',
      manufacturingDate: b.manufacturingDate || '',
      expiryDate: b.expiryDate || '',
    });
    setErrors({});
    setModalOpen(true);
  };

  const validate = () => {
    const e = {};
    if (!form.batchNumber?.trim()) e.batchNumber = 'Batch number is required';
    if (!form.medicine) e.medicine = 'Medicine is required';
    if (!form.supplier) e.supplier = 'Supplier is required';
    if (!form.unitCost || parseFloat(form.unitCost) <= 0) e.unitCost = 'Unit cost must be > 0';
    if (!form.unitPrice || parseFloat(form.unitPrice) <= 0) e.unitPrice = 'Unit price must be > 0';
    if (!form.manufacturingDate) e.manufacturingDate = 'Required';
    if (!form.expiryDate) e.expiryDate = 'Required';
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSave = async () => {
    if (!validate()) return;
    const payload = {
      ...form,
      unitCost: parseFloat(form.unitCost),
      unitPrice: parseFloat(form.unitPrice),
      medicine: medicines.find(m => m.id == form.medicine) || null,
      supplier: suppliers.find(s => s.id == form.supplier) || null,
    };
    try {
      if (editing) { await inventoryBatchApi.update(editing.id, payload); toast.success('Batch updated'); }
      else { await inventoryBatchApi.create(payload); toast.success('Batch created'); }
      setModalOpen(false); loadData();
    } catch (err) { toast.error(err.message); }
  };

  const handleDelete = async () => {
    try { await inventoryBatchApi.delete(confirmDelete.id); toast.success('Batch deleted'); setConfirmDelete(null); loadData(); }
    catch (err) { toast.error(err.message); }
  };

  return (
    <>
      <div className="page-header">
        <div><h1>Inventory Batches</h1><div className="page-header-subtitle">Track medicine batches and expiry dates</div></div>
        <button className="btn btn-primary" onClick={openCreate}>+ Add Batch</button>
      </div>
      <div className="page-body">
        <div className="toolbar">
          <div className="search-box">
            <span className="search-icon">🔍</span>
            <input placeholder="Search by batch number..." value={search} onChange={(e) => setSearch(e.target.value)} />
          </div>
        </div>
        <div className="card">
          <div className="table-wrapper">
            <table>
              <thead><tr><th>Batch #</th><th>Medicine</th><th>Supplier</th><th>Received</th><th>Remaining</th><th>Unit Cost</th><th>Unit Price</th><th>Expiry</th><th>Status</th><th>Actions</th></tr></thead>
              <tbody>
                {loading ? (
                  <tr><td colSpan="10"><div className="loading-spinner"><div className="spinner" /></div></td></tr>
                ) : filtered.length === 0 ? (
                  <tr><td colSpan="10"><div className="empty-state"><div className="icon">📦</div><h3>No batches found</h3></div></td></tr>
                ) : filtered.map(b => (
                  <tr key={b.id}>
                    <td><strong>{b.batchNumber}</strong></td>
                    <td>{b.medicine?.name || '-'}</td>
                    <td>{b.supplier?.name || '-'}</td>
                    <td>{b.quantity}</td>
                    <td>{b.quantityRemaining}</td>
                    <td>${parseFloat(b.unitCost).toFixed(2)}</td>
                    <td>{b.unitPrice != null ? `$${parseFloat(b.unitPrice).toFixed(2)}` : '-'}</td>
                    <td>{b.expiryDate || '-'}</td>
                    <td>
                      {b.expired ? <span className="badge badge-red">Expired</span> :
                       new Date(b.expiryDate) < new Date(Date.now() + 30*86400000) ? <span className="badge badge-amber">Expiring</span> :
                       <span className="badge badge-green">OK</span>}
                    </td>
                    <td>
                      <button className="btn btn-ghost btn-sm" onClick={() => openEdit(b)}>✏️</button>
                      <button className="btn btn-ghost btn-sm" style={{ color: 'var(--danger)' }} onClick={() => setConfirmDelete(b)}>🗑️</button>
                    </td>
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
              <h2>{editing ? 'Edit Batch' : 'Add Batch'}</h2>
              <button className="modal-close" onClick={() => setModalOpen(false)}>×</button>
            </div>
            <div className="modal-body">
              <div className="form-group">
                <label className="form-label">Batch Number <span className="required">*</span></label>
                <input className={`form-input ${errors.batchNumber ? 'error' : ''}`} value={form.batchNumber || ''} onChange={e => setForm({...form, batchNumber: e.target.value})} />
                {errors.batchNumber && <div className="form-error">{errors.batchNumber}</div>}
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">Medicine <span className="required">*</span></label>
                  <select className={`form-select ${errors.medicine ? 'error' : ''}`} value={form.medicine || ''} onChange={e => setForm({...form, medicine: e.target.value})}>
                    <option value="">Select medicine</option>
                    {medicines.map(m => <option key={m.id} value={m.id}>{m.name}</option>)}
                  </select>
                  {errors.medicine && <div className="form-error">{errors.medicine}</div>}
                </div>
                <div className="form-group">
                  <label className="form-label">Supplier <span className="required">*</span></label>
                  <select className={`form-select ${errors.supplier ? 'error' : ''}`} value={form.supplier || ''} onChange={e => setForm({...form, supplier: e.target.value})}>
                    <option value="">Select supplier</option>
                    {suppliers.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
                  </select>
                  {errors.supplier && <div className="form-error">{errors.supplier}</div>}
                </div>
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">Unit Cost <span className="required">*</span></label>
                  <input className={`form-input ${errors.unitCost ? 'error' : ''}`} type="number" step="0.01" min="0" value={form.unitCost || ''} onChange={e => setForm({...form, unitCost: e.target.value})} />
                  {errors.unitCost && <div className="form-error">{errors.unitCost}</div>}
                </div>
                <div className="form-group">
                  <label className="form-label">Unit Price <span className="required">*</span></label>
                  <input className={`form-input ${errors.unitPrice ? 'error' : ''}`} type="number" step="0.01" min="0" value={form.unitPrice || ''} onChange={e => setForm({...form, unitPrice: e.target.value})} />
                  {errors.unitPrice && <div className="form-error">{errors.unitPrice}</div>}
                </div>
              </div>
              <div className="form-group">
                <div className="form-hint" style={{ fontSize: 12, color: 'var(--text-secondary)', background: 'var(--bg-secondary)', padding: '8px 12px', borderRadius: 6 }}>
                  📦 Quantity starts at 0 and is updated automatically by <strong>Stock In</strong> / <strong>Stock Out</strong> movements.
                </div>
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">Manufacturing Date <span className="required">*</span></label>
                  <input className={`form-input ${errors.manufacturingDate ? 'error' : ''}`} type="date" value={form.manufacturingDate || ''} onChange={e => setForm({...form, manufacturingDate: e.target.value})} />
                  {errors.manufacturingDate && <div className="form-error">{errors.manufacturingDate}</div>}
                </div>
                <div className="form-group">
                  <label className="form-label">Expiry Date <span className="required">*</span></label>
                  <input className={`form-input ${errors.expiryDate ? 'error' : ''}`} type="date" value={form.expiryDate || ''} onChange={e => setForm({...form, expiryDate: e.target.value})} />
                  {errors.expiryDate && <div className="form-error">{errors.expiryDate}</div>}
                </div>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn btn-secondary" onClick={() => setModalOpen(false)}>Cancel</button>
              <button className="btn btn-primary" onClick={handleSave}>{editing ? 'Update' : 'Create'}</button>
            </div>
          </div>
        </div>
      )}

      <ConfirmDialog open={!!confirmDelete} title="Delete Batch" message={`Delete batch "${confirmDelete?.batchNumber}"?`} onConfirm={handleDelete} onCancel={() => setConfirmDelete(null)} />
    </>
  );
}
