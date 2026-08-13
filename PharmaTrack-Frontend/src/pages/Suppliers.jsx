/**
 * Suppliers Page
 * Full CRUD management for pharmaceutical suppliers.
 */
import { useState, useEffect } from 'react';
import { supplierApi } from '../api/api';
import { useToast } from '../components/Toast';
import ConfirmDialog from '../components/ConfirmDialog';

const emptyForm = { code: '', name: '', contactPerson: '', phone: '', email: '', address: '', city: '', country: '', active: true };

export default function Suppliers() {
  const toast = useToast();
  const [suppliers, setSuppliers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [errors, setErrors] = useState({});
  const [confirmDelete, setConfirmDelete] = useState(null);

  const loadData = async () => {
    try { setLoading(true); setSuppliers(await supplierApi.getAll()); }
    catch (err) { toast.error(err.message); }
    finally { setLoading(false); }
  };

  useEffect(() => { loadData(); }, []);

  const filtered = suppliers.filter(s =>
    s.name?.toLowerCase().includes(search.toLowerCase()) ||
    s.code?.toLowerCase().includes(search.toLowerCase())
  );

  const openCreate = () => { setEditing(null); setForm(emptyForm); setErrors({}); setModalOpen(true); };
  const openEdit = (s) => { setEditing(s); setForm({ ...s }); setErrors({}); setModalOpen(true); };

  const validate = () => {
    const e = {};
    if (!form.code?.trim()) e.code = 'Code is required';
    if (!form.name?.trim()) e.name = 'Name is required';
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSave = async () => {
    if (!validate()) return;
    try {
      if (editing) { await supplierApi.update(editing.id, form); toast.success('Supplier updated'); }
      else { await supplierApi.create(form); toast.success('Supplier created'); }
      setModalOpen(false); loadData();
    } catch (err) { toast.error(err.message); }
  };

  const handleDelete = async () => {
    try { await supplierApi.delete(confirmDelete.id); toast.success('Supplier deleted'); setConfirmDelete(null); loadData(); }
    catch (err) { toast.error(err.message); }
  };

  return (
    <>
      <div className="page-header">
        <div><h1>Suppliers</h1><div className="page-header-subtitle">Manage pharmaceutical suppliers</div></div>
        <button className="btn btn-primary" onClick={openCreate}>+ Add Supplier</button>
      </div>
      <div className="page-body">
        <div className="toolbar">
          <div className="search-box">
            <span className="search-icon">🔍</span>
            <input placeholder="Search suppliers..." value={search} onChange={(e) => setSearch(e.target.value)} />
          </div>
        </div>
        <div className="card">
          <div className="table-wrapper">
            <table>
              <thead><tr><th>Code</th><th>Name</th><th>Contact</th><th>Phone</th><th>Email</th><th>City</th><th>Country</th><th>Status</th><th>Actions</th></tr></thead>
              <tbody>
                {loading ? (
                  <tr><td colSpan="9"><div className="loading-spinner"><div className="spinner" /></div></td></tr>
                ) : filtered.length === 0 ? (
                  <tr><td colSpan="9"><div className="empty-state"><div className="icon">🚚</div><h3>No suppliers found</h3></div></td></tr>
                ) : filtered.map(s => (
                  <tr key={s.id}>
                    <td><strong>{s.code}</strong></td>
                    <td>{s.name}</td>
                    <td>{s.contactPerson || '-'}</td>
                    <td>{s.phone || '-'}</td>
                    <td>{s.email || '-'}</td>
                    <td>{s.city || '-'}</td>
                    <td>{s.country || '-'}</td>
                    <td><span className={`badge badge-${s.active ? 'green' : 'red'}`}>{s.active ? 'Active' : 'Inactive'}</span></td>
                    <td>
                      <button className="btn btn-ghost btn-sm" onClick={() => openEdit(s)}>✏️</button>
                      <button className="btn btn-ghost btn-sm" style={{ color: 'var(--danger)' }} onClick={() => setConfirmDelete(s)}>🗑️</button>
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
              <h2>{editing ? 'Edit Supplier' : 'Add Supplier'}</h2>
              <button className="modal-close" onClick={() => setModalOpen(false)}>×</button>
            </div>
            <div className="modal-body">
              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">Code <span className="required">*</span></label>
                  <input className={`form-input ${errors.code ? 'error' : ''}`} value={form.code || ''} onChange={e => setForm({...form, code: e.target.value})} />
                  {errors.code && <div className="form-error">{errors.code}</div>}
                </div>
                <div className="form-group">
                  <label className="form-label">Name <span className="required">*</span></label>
                  <input className={`form-input ${errors.name ? 'error' : ''}`} value={form.name || ''} onChange={e => setForm({...form, name: e.target.value})} />
                  {errors.name && <div className="form-error">{errors.name}</div>}
                </div>
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">Contact Person</label>
                  <input className="form-input" value={form.contactPerson || ''} onChange={e => setForm({...form, contactPerson: e.target.value})} />
                </div>
                <div className="form-group">
                  <label className="form-label">Phone</label>
                  <input className="form-input" value={form.phone || ''} onChange={e => setForm({...form, phone: e.target.value})} />
                </div>
              </div>
              <div className="form-group">
                <label className="form-label">Email</label>
                <input className="form-input" type="email" value={form.email || ''} onChange={e => setForm({...form, email: e.target.value})} />
              </div>
              <div className="form-group">
                <label className="form-label">Address</label>
                <input className="form-input" value={form.address || ''} onChange={e => setForm({...form, address: e.target.value})} />
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">City</label>
                  <input className="form-input" value={form.city || ''} onChange={e => setForm({...form, city: e.target.value})} />
                </div>
                <div className="form-group">
                  <label className="form-label">Country</label>
                  <input className="form-input" value={form.country || ''} onChange={e => setForm({...form, country: e.target.value})} />
                </div>
              </div>
              <div className="form-group">
                <label className="form-checkbox">
                  <input type="checkbox" checked={form.active ?? true} onChange={e => setForm({...form, active: e.target.checked})} />
                  Active
                </label>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn btn-secondary" onClick={() => setModalOpen(false)}>Cancel</button>
              <button className="btn btn-primary" onClick={handleSave}>{editing ? 'Update' : 'Create'}</button>
            </div>
          </div>
        </div>
      )}

      <ConfirmDialog open={!!confirmDelete} title="Delete Supplier" message={`Delete "${confirmDelete?.name}"?`} onConfirm={handleDelete} onCancel={() => setConfirmDelete(null)} />
    </>
  );
}
