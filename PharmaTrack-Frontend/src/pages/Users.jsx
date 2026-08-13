/**
 * Users Page
 * Full CRUD management for system users with role filtering and search.
 */
import { useState, useEffect } from 'react';
import { userApi } from '../api/api';
import { useToast } from '../components/Toast';
import ConfirmDialog from '../components/ConfirmDialog';

const ROLES = ['ADMIN', 'PHARMACIST', 'CASHIER', 'INVENTORY_MANAGER', 'PROCUREMENT_OFFICER', 'AUDITOR'];
const ROLE_COLORS = {
  ADMIN: 'red', PHARMACIST: 'teal', CASHIER: 'indigo',
  INVENTORY_MANAGER: 'green', PROCUREMENT_OFFICER: 'amber', AUDITOR: 'gray'
};

const emptyForm = { username: '', password: '', email: '', fullName: '', role: 'PHARMACIST', active: true };

export default function Users() {
  const toast = useToast();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [errors, setErrors] = useState({});
  const [confirmDelete, setConfirmDelete] = useState(null);

  const loadUsers = async () => {
    try {
      setLoading(true);
      const data = roleFilter ? await userApi.getByRole(roleFilter) : await userApi.getAll();
      setUsers(data);
    } catch (err) { toast.error(err.message); }
    finally { setLoading(false); }
  };

  useEffect(() => { loadUsers(); }, [roleFilter]);

  const filtered = users.filter(u =>
    u.username?.toLowerCase().includes(search.toLowerCase()) ||
    u.email?.toLowerCase().includes(search.toLowerCase()) ||
    u.fullName?.toLowerCase().includes(search.toLowerCase())
  );

  const openCreate = () => { setEditing(null); setForm(emptyForm); setErrors({}); setModalOpen(true); };
  const openEdit = (u) => { setEditing(u); setForm({ ...u }); setErrors({}); setModalOpen(true); };

  const validate = () => {
    const e = {};
    if (!form.username?.trim()) e.username = 'Username is required';
    else if (form.username.trim().length < 3) e.username = 'Must be at least 3 characters';
    else if (form.username.trim().length > 50) e.username = 'Must not exceed 50 characters';
    if (!editing && !form.password?.trim()) e.password = 'Password is required';
    if (form.password && form.password.length < 6) e.password = 'Must be at least 6 characters';
    if (!form.email?.trim()) e.email = 'Email is required';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) e.email = 'Invalid email';
    if (!form.fullName?.trim()) e.fullName = 'Full name is required';
    if (!form.role) e.role = 'Role is required';
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSave = async () => {
    if (!validate()) return;
    try {
      if (editing) {
        await userApi.update(editing.id, form);
        toast.success('User updated successfully');
      } else {
        await userApi.create(form);
        toast.success('User created successfully');
      }
      setModalOpen(false);
      loadUsers();
    } catch (err) {
      if (err.validationErrors) {
        setErrors(err.validationErrors);
      } else {
        toast.error(err.message);
      }
    }
  };

  const handleDelete = async () => {
    try {
      await userApi.delete(confirmDelete.id);
      toast.success('User deleted');
      setConfirmDelete(null);
      loadUsers();
    } catch (err) { toast.error(err.message); }
  };

  return (
    <>
      <div className="page-header">
        <div><h1>Users</h1><div className="page-header-subtitle">Manage system users and roles</div></div>
        <button className="btn btn-primary" onClick={openCreate}>+ Add User</button>
      </div>
      <div className="page-body">
        <div className="toolbar">
          <div className="search-box">
            <span className="search-icon">🔍</span>
            <input placeholder="Search users..." value={search} onChange={(e) => setSearch(e.target.value)} />
          </div>
          <select className="form-select" style={{ width: 180 }} value={roleFilter} onChange={(e) => setRoleFilter(e.target.value)}>
            <option value="">All Roles</option>
            {ROLES.map(r => <option key={r} value={r}>{r.replace(/_/g, ' ')}</option>)}
          </select>
        </div>
        <div className="card">
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>ID</th><th>Username</th><th>Full Name</th><th>Email</th><th>Role</th><th>Status</th><th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr><td colSpan="7"><div className="loading-spinner"><div className="spinner" /></div></td></tr>
                ) : filtered.length === 0 ? (
                  <tr><td colSpan="7"><div className="empty-state"><div className="icon">👤</div><h3>No users found</h3></div></td></tr>
                ) : filtered.map(u => (
                  <tr key={u.id}>
                    <td>{u.id}</td>
                    <td><strong>{u.username}</strong></td>
                    <td>{u.fullName}</td>
                    <td>{u.email}</td>
                    <td><span className={`badge badge-${ROLE_COLORS[u.role] || 'gray'}`}>{u.role?.replace(/_/g, ' ')}</span></td>
                    <td><span className={`badge badge-${u.active ? 'green' : 'red'}`}>{u.active ? 'Active' : 'Inactive'}</span></td>
                    <td>
                      <button className="btn btn-ghost btn-sm" onClick={() => openEdit(u)}>✏️ Edit</button>
                      <button className="btn btn-ghost btn-sm" style={{ color: 'var(--danger)' }} onClick={() => setConfirmDelete(u)}>🗑️</button>
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
              <h2>{editing ? 'Edit User' : 'Create User'}</h2>
              <button className="modal-close" onClick={() => setModalOpen(false)}>×</button>
            </div>
            <div className="modal-body">
              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">Username <span className="required">*</span></label>
                  <input className={`form-input ${errors.username ? 'error' : ''}`} value={form.username || ''} onChange={e => setForm({...form, username: e.target.value})} />
                  {errors.username && <div className="form-error">{errors.username}</div>}
                </div>
                <div className="form-group">
                  <label className="form-label">Email <span className="required">*</span></label>
                  <input className={`form-input ${errors.email ? 'error' : ''}`} type="email" value={form.email || ''} onChange={e => setForm({...form, email: e.target.value})} />
                  {errors.email && <div className="form-error">{errors.email}</div>}
                </div>
              </div>
              {!editing && (
                <div className="form-group">
                  <label className="form-label">Password <span className="required">*</span></label>
                  <input className={`form-input ${errors.password ? 'error' : ''}`} type="password" value={form.password || ''} onChange={e => setForm({...form, password: e.target.value})} />
                  {errors.password && <div className="form-error">{errors.password}</div>}
                </div>
              )}
              <div className="form-group">
                <label className="form-label">Full Name <span className="required">*</span></label>
                <input className={`form-input ${errors.fullName ? 'error' : ''}`} value={form.fullName || ''} onChange={e => setForm({...form, fullName: e.target.value})} />
                {errors.fullName && <div className="form-error">{errors.fullName}</div>}
              </div>
              <div className="form-group">
                <label className="form-label">Role <span className="required">*</span></label>
                <select className={`form-select ${errors.role ? 'error' : ''}`} value={form.role || ''} onChange={e => setForm({...form, role: e.target.value})}>
                  {ROLES.map(r => <option key={r} value={r}>{r.replace(/_/g, ' ')}</option>)}
                </select>
                {errors.role && <div className="form-error">{errors.role}</div>}
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

      <ConfirmDialog open={!!confirmDelete} title="Delete User" message={`Are you sure you want to delete "${confirmDelete?.username}"?`} onConfirm={handleDelete} onCancel={() => setConfirmDelete(null)} />
    </>
  );
}
