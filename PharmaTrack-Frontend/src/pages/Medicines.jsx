/**
 * Medicines Page
 * Full CRUD management for medicines with search, category filtering,
 * and manufacturer association.
 */
import { useState, useEffect } from 'react';
import { medicineApi, categoryApi, manufacturerApi } from '../api/api';
import { useToast } from '../components/Toast';
import ConfirmDialog from '../components/ConfirmDialog';

const emptyForm = {
  code: '', name: '', description: '', category: null, manufacturer: null,
  unit: '', requiresPrescription: false, active: true
};

export default function Medicines() {
  const toast = useToast();
  const [medicines, setMedicines] = useState([]);
  const [categories, setCategories] = useState([]);
  const [manufacturers, setManufacturers] = useState([]);
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
      const meds = await medicineApi.getAll().catch(e => { toast.error('Failed to load medicines'); return []; });
      const cats = await categoryApi.getAll().catch(e => { toast.error('Failed to load categories'); return []; });
      const mfgs = await manufacturerApi.getAll().catch(e => { toast.error('Failed to load manufacturers'); return []; });
      setMedicines(meds);
      setCategories(cats);
      setManufacturers(mfgs);
    } finally { setLoading(false); }
  };

  useEffect(() => { loadData(); }, []);

  const filtered = medicines.filter(m =>
    m.name?.toLowerCase().includes(search.toLowerCase()) ||
    m.code?.toLowerCase().includes(search.toLowerCase())
  );

  const openCreate = () => { setEditing(null); setForm(emptyForm); setErrors({}); setModalOpen(true); };
  const openEdit = (m) => {
    setEditing(m);
    setForm({
      ...m,
      category: m.category ? m.category.id : '',
      manufacturer: m.manufacturer ? m.manufacturer.id : ''
    });
    setErrors({});
    setModalOpen(true);
  };

  const validate = () => {
    const e = {};
    if (!form.code?.trim()) e.code = 'Code is required';
    if (!form.name?.trim()) e.name = 'Name is required';
    if (!form.unit?.trim()) e.unit = 'Unit is required';
    if (!form.category) e.category = 'Category is required';
    if (!form.manufacturer) e.manufacturer = 'Manufacturer is required';
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSave = async () => {
    if (!validate()) return;
    const payload = {
      ...form,
      category: categories.find(c => c.id == form.category) || null,
      manufacturer: manufacturers.find(m => m.id == form.manufacturer) || null,
    };
    try {
      if (editing) {
        await medicineApi.update(editing.id, payload);
        toast.success('Medicine updated');
      } else {
        await medicineApi.create(payload);
        toast.success('Medicine created');
      }
      setModalOpen(false);
      loadData();
    } catch (err) { toast.error(err.message); }
  };

  const handleDelete = async () => {
    try {
      await medicineApi.delete(confirmDelete.id);
      toast.success('Medicine deleted');
      setConfirmDelete(null);
      loadData();
    } catch (err) { toast.error(err.message); }
  };

  return (
    <>
      <div className="page-header">
        <div><h1>Medicines</h1><div className="page-header-subtitle">Manage medicine catalog</div></div>
        <button className="btn btn-primary" onClick={openCreate}>+ Add Medicine</button>
      </div>
      <div className="page-body">
        <div className="toolbar">
          <div className="search-box">
            <span className="search-icon">🔍</span>
            <input placeholder="Search by name or code..." value={search} onChange={(e) => setSearch(e.target.value)} />
          </div>
        </div>
        <div className="card">
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>Code</th><th>Name</th><th>Category</th><th>Manufacturer</th><th>Unit</th><th>Rx</th><th>Status</th><th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr><td colSpan="8"><div className="loading-spinner"><div className="spinner" /></div></td></tr>
                ) : filtered.length === 0 ? (
                  <tr><td colSpan="8"><div className="empty-state"><div className="icon">💊</div><h3>No medicines found</h3></div></td></tr>
                ) : filtered.map(m => (
                  <tr key={m.id}>
                    <td><strong>{m.code}</strong></td>
                    <td>{m.name}</td>
                    <td>{m.category?.name || '-'}</td>
                    <td>{m.manufacturer?.name || '-'}</td>
                    <td>{m.unit}</td>
                    <td>{m.requiresPrescription ? <span className="badge badge-amber">Rx</span> : <span className="badge badge-gray">OTC</span>}</td>
                    <td><span className={`badge badge-${m.active ? 'green' : 'red'}`}>{m.active ? 'Active' : 'Inactive'}</span></td>
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
              <h2>{editing ? 'Edit Medicine' : 'Add Medicine'}</h2>
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
              <div className="form-group">
                <label className="form-label">Description</label>
                <textarea className="form-textarea" value={form.description || ''} onChange={e => setForm({...form, description: e.target.value})} />
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">Category <span className="required">*</span></label>
                  <select className={`form-select ${errors.category ? 'error' : ''}`} value={form.category || ''} onChange={e => setForm({...form, category: e.target.value})}>
                    <option value="">Select category</option>
                    {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                  </select>
                  {errors.category && <div className="form-error">{errors.category}</div>}
                </div>
                <div className="form-group">
                  <label className="form-label">Manufacturer <span className="required">*</span></label>
                  <select className={`form-select ${errors.manufacturer ? 'error' : ''}`} value={form.manufacturer || ''} onChange={e => setForm({...form, manufacturer: e.target.value})}>
                    <option value="">Select manufacturer</option>
                    {manufacturers.map(m => <option key={m.id} value={m.id}>{m.name}</option>)}
                  </select>
                  {errors.manufacturer && <div className="form-error">{errors.manufacturer}</div>}
                </div>
              </div>
              <div className="form-group">
                <label className="form-label">Unit <span className="required">*</span></label>
                <input className={`form-input ${errors.unit ? 'error' : ''}`} value={form.unit || ''} onChange={e => setForm({...form, unit: e.target.value})} placeholder="e.g. tablets, ml, capsules" />
                {errors.unit && <div className="form-error">{errors.unit}</div>}
              </div>
              <div className="form-group">
                <label className="form-checkbox">
                  <input type="checkbox" checked={form.requiresPrescription || false} onChange={e => setForm({...form, requiresPrescription: e.target.checked})} />
                  Requires Prescription
                </label>
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

      <ConfirmDialog open={!!confirmDelete} title="Delete Medicine" message={`Delete "${confirmDelete?.name}"? This cannot be undone.`} onConfirm={handleDelete} onCancel={() => setConfirmDelete(null)} />
    </>
  );
}
