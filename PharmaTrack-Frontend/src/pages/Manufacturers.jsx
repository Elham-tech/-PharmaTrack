/**
 * Manufacturers Page
 * Full CRUD management for medicine manufacturers.
 */
import { useState, useEffect } from 'react';
import { manufacturerApi } from '../api/api';
import { useToast } from '../components/Toast';
import ConfirmDialog from '../components/ConfirmDialog';

const emptyForm = { name: '', address: '', phone: '', email: '', country: '' };

export default function Manufacturers() {
  const toast = useToast();
  const [manufacturers, setManufacturers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [errors, setErrors] = useState({});
  const [confirmDelete, setConfirmDelete] = useState(null);

  const loadData = async () => {
    try { setLoading(true); setManufacturers(await manufacturerApi.getAll()); }
    catch (err) { toast.error(err.message); }
    finally { setLoading(false); }
  };

  useEffect(() => { loadData(); }, []);

  const filtered = manufacturers.filter(m => m.name?.toLowerCase().includes(search.toLowerCase()));

  const openCreate = () => { setEditing(null); setForm(emptyForm); setErrors({}); setModalOpen(true); };
  const openEdit = (m) => { setEditing(m); setForm({ ...m }); setErrors({}); setModalOpen(true); };

  const validate = () => {
    const e = {};
    if (!form.name?.trim()) e.name = 'Name is required';
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSave = async () => {
    if (!validate()) return;
    try {
      if (editing) { await manufacturerApi.update(editing.id, form); toast.success('Manufacturer updated'); }
      else { await manufacturerApi.create(form); toast.success('Manufacturer created'); }
      setModalOpen(false); loadData();
    } catch (err) { toast.error(err.message); }
  };

  const handleDelete = async () => {
    try { await manufacturerApi.delete(confirmDelete.id); toast.success('Manufacturer deleted'); setConfirmDelete(null); loadData(); }
    catch (err) { toast.error(err.message); }
  };

  return (
    <>
      <div className="page-header">
        <div><h1>Manufacturers</h1><div className="page-header-subtitle">Manage medicine manufacturers</div></div>
        <button className="btn btn-primary" onClick={openCreate}>+ Add Manufacturer</button>
      </div>
      <div className="page-body">
        <div className="toolbar">
          <div className="search-box">
            <span className="search-icon">🔍</span>
            <input placeholder="Search manufacturers..." value={search} onChange={(e) => setSearch(e.target.value)} />
          </div>
        </div>
        <div className="card">
          <div className="table-wrapper">
            <table>
              <thead><tr><th>ID</th><th>Name</th><th>Address</th><th>Phone</th><th>Email</th><th>Country</th><th>Actions</th></tr></thead>
              <tbody>
                {loading ? (
                  <tr><td colSpan="7"><div className="loading-spinner"><div className="spinner" /></div></td></tr>
                ) : filtered.length === 0 ? (
                  <tr><td colSpan="7"><div className="empty-state"><div className="icon">🏭</div><h3>No manufacturers found</h3></div></td></tr>
                ) : filtered.map(m => (
                  <tr key={m.id}>
                    <td>{m.id}</td>
                    <td><strong>{m.name}</strong></td>
                    <td>{m.address || '-'}</td>
                    <td>{m.phone || '-'}</td>
                    <td>{m.email || '-'}</td>
                    <td>{m.country || '-'}</td>
                    <td>
                      <button className="btn btn-ghost btn-sm" onClick={() => openEdit(m)}>✏️</button>
                      <button className="btn btn-ghost btn-sm" style={{ color: 'var(--danger)' }} onClick={() => setConfirmDelete(m)}>🗑️</button>
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
              <h2>{editing ? 'Edit Manufacturer' : 'Add Manufacturer'}</h2>
              <button className="modal-close" onClick={() => setModalOpen(false)}>×</button>
            </div>
            <div className="modal-body">
              <div className="form-group">
                <label className="form-label">Name <span className="required">*</span></label>
                <input className={`form-input ${errors.name ? 'error' : ''}`} value={form.name || ''} onChange={e => setForm({...form, name: e.target.value})} />
                {errors.name && <div className="form-error">{errors.name}</div>}
              </div>
              <div className="form-group">
                <label className="form-label">Address</label>
                <input className="form-input" value={form.address || ''} onChange={e => setForm({...form, address: e.target.value})} />
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">Phone</label>
                  <input className="form-input" value={form.phone || ''} onChange={e => setForm({...form, phone: e.target.value})} />
                </div>
                <div className="form-group">
                  <label className="form-label">Email</label>
                  <input className="form-input" type="email" value={form.email || ''} onChange={e => setForm({...form, email: e.target.value})} />
                </div>
              </div>
              <div className="form-group">
                <label className="form-label">Country</label>
                <input className="form-input" value={form.country || ''} onChange={e => setForm({...form, country: e.target.value})} />
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn btn-secondary" onClick={() => setModalOpen(false)}>Cancel</button>
              <button className="btn btn-primary" onClick={handleSave}>{editing ? 'Update' : 'Create'}</button>
            </div>
          </div>
        </div>
      )}

      <ConfirmDialog open={!!confirmDelete} title="Delete Manufacturer" message={`Delete "${confirmDelete?.name}"?`} onConfirm={handleDelete} onCancel={() => setConfirmDelete(null)} />
    </>
  );
}
