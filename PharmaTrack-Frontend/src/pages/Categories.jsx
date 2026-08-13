/**
 * Categories Page
 * Full CRUD management for medicine categories.
 */
import { useState, useEffect } from 'react';
import { categoryApi } from '../api/api';
import { useToast } from '../components/Toast';
import ConfirmDialog from '../components/ConfirmDialog';

const emptyForm = { name: '', description: '' };

export default function Categories() {
  const toast = useToast();
  const [categories, setCategories] = useState([]);
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
      setCategories(await categoryApi.getAll());
    } catch (err) { toast.error(err.message); }
    finally { setLoading(false); }
  };

  useEffect(() => { loadData(); }, []);

  const filtered = categories.filter(c =>
    c.name?.toLowerCase().includes(search.toLowerCase())
  );

  const openCreate = () => { setEditing(null); setForm(emptyForm); setErrors({}); setModalOpen(true); };
  const openEdit = (c) => { setEditing(c); setForm({ name: c.name, description: c.description || '' }); setErrors({}); setModalOpen(true); };

  const validate = () => {
    const e = {};
    if (!form.name?.trim()) e.name = 'Name is required';
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSave = async () => {
    if (!validate()) return;
    try {
      if (editing) {
        await categoryApi.update(editing.id, form);
        toast.success('Category updated');
      } else {
        await categoryApi.create(form);
        toast.success('Category created');
      }
      setModalOpen(false);
      loadData();
    } catch (err) { toast.error(err.message); }
  };

  const handleDelete = async () => {
    try {
      await categoryApi.delete(confirmDelete.id);
      toast.success('Category deleted');
      setConfirmDelete(null);
      loadData();
    } catch (err) { toast.error(err.message); }
  };

  return (
    <>
      <div className="page-header">
        <div><h1>Categories</h1><div className="page-header-subtitle">Organize medicines by category</div></div>
        <button className="btn btn-primary" onClick={openCreate}>+ Add Category</button>
      </div>
      <div className="page-body">
        <div className="toolbar">
          <div className="search-box">
            <span className="search-icon">🔍</span>
            <input placeholder="Search categories..." value={search} onChange={(e) => setSearch(e.target.value)} />
          </div>
        </div>
        <div className="card">
          <div className="table-wrapper">
            <table>
              <thead><tr><th>ID</th><th>Name</th><th>Description</th><th>Created</th><th>Actions</th></tr></thead>
              <tbody>
                {loading ? (
                  <tr><td colSpan="5"><div className="loading-spinner"><div className="spinner" /></div></td></tr>
                ) : filtered.length === 0 ? (
                  <tr><td colSpan="5"><div className="empty-state"><div className="icon">🏷️</div><h3>No categories found</h3></div></td></tr>
                ) : filtered.map(c => (
                  <tr key={c.id}>
                    <td>{c.id}</td>
                    <td><strong>{c.name}</strong></td>
                    <td>{c.description || '-'}</td>
                    <td>{c.createdAt ? new Date(c.createdAt).toLocaleDateString() : '-'}</td>
                    <td>
                      <button className="btn btn-ghost btn-sm" onClick={() => openEdit(c)}>✏️</button>
                      <button className="btn btn-ghost btn-sm" style={{ color: 'var(--danger)' }} onClick={() => setConfirmDelete(c)}>🗑️</button>
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
              <h2>{editing ? 'Edit Category' : 'Add Category'}</h2>
              <button className="modal-close" onClick={() => setModalOpen(false)}>×</button>
            </div>
            <div className="modal-body">
              <div className="form-group">
                <label className="form-label">Name <span className="required">*</span></label>
                <input className={`form-input ${errors.name ? 'error' : ''}`} value={form.name} onChange={e => setForm({...form, name: e.target.value})} />
                {errors.name && <div className="form-error">{errors.name}</div>}
              </div>
              <div className="form-group">
                <label className="form-label">Description</label>
                <textarea className="form-textarea" value={form.description} onChange={e => setForm({...form, description: e.target.value})} />
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn btn-secondary" onClick={() => setModalOpen(false)}>Cancel</button>
              <button className="btn btn-primary" onClick={handleSave}>{editing ? 'Update' : 'Create'}</button>
            </div>
          </div>
        </div>
      )}

      <ConfirmDialog open={!!confirmDelete} title="Delete Category" message={`Delete "${confirmDelete?.name}"?`} onConfirm={handleDelete} onCancel={() => setConfirmDelete(null)} />
    </>
  );
}
