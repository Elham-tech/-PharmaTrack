/**
 * Prescriptions Page
 * Management for prescriptions with structured medicine items.
 * Dispensing happens automatically when a prescription is created - every item is
 * dispensed from available stock and the cashier then approves (PAID) or voids (VOIDED)
 * the resulting dispensing records.
 */
import { useState, useEffect } from 'react';
import { prescriptionApi, medicineApi } from '../api/api';
import { useToast } from '../components/Toast';
import ConfirmDialog from '../components/ConfirmDialog';

const emptyForm = {
  prescriptionNumber: '', patientName: '', patientIdNumber: '', doctorName: '',
  hospitalName: '', prescriptionDetails: '', items: []
};

export default function Prescriptions() {
  const toast = useToast();
  const [prescriptions, setPrescriptions] = useState([]);
  const [medicines, setMedicines] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [errors, setErrors] = useState({});
  const [confirmDelete, setConfirmDelete] = useState(null);

  const loadData = async () => {
    try {
      setLoading(true);
      const rx = await prescriptionApi.getAll().catch(e => { toast.error('Failed to load prescriptions'); return []; });
      const med = await medicineApi.getAll().catch(e => { toast.error('Failed to load medicines'); return []; });
      setPrescriptions(rx); setMedicines(med);
    } finally { setLoading(false); }
  };

  useEffect(() => { loadData(); }, []);

  const filtered = prescriptions.filter(p => {
    const matchSearch = !search ||
      p.patientName?.toLowerCase().includes(search.toLowerCase()) ||
      p.prescriptionNumber?.toLowerCase().includes(search.toLowerCase()) ||
      p.doctorName?.toLowerCase().includes(search.toLowerCase());
    const matchStatus = statusFilter === '' ||
      (statusFilter === 'dispensed' && p.dispensed) ||
      (statusFilter === 'pending' && !p.dispensed && !p.voided) ||
      (statusFilter === 'voided' && p.voided);
    return matchSearch && matchStatus;
  });

  const openCreate = () => { setEditing(null); setForm({ ...emptyForm, items: [] }); setErrors({}); setModalOpen(true); };
  const openEdit = (p) => {
    setEditing(p);
    setForm({
      ...p,
      items: (p.items || []).map(item => ({
        medicineId: item.medicine?.id || '',
        quantity: item.quantity || 1,
        dosage: item.dosage || '',
        timesPerDay: item.timesPerDay || 1,
        durationDays: item.durationDays || 1,
        notes: item.notes || ''
      }))
    });
    setErrors({});
    setModalOpen(true);
  };

  const addItem = () => {
    setForm(f => ({
      ...f,
      items: [...f.items, { medicineId: '', quantity: 1, dosage: '', timesPerDay: 1, durationDays: 1, notes: '' }]
    }));
  };

  const removeItem = (index) => {
    setForm(f => ({
      ...f,
      items: f.items.filter((_, i) => i !== index)
    }));
  };

  const updateItem = (index, field, value) => {
    setForm(f => ({
      ...f,
      items: f.items.map((item, i) => i === index ? { ...item, [field]: value } : item)
    }));
  };

  const adjustItemField = (index, field, delta) => {
    setForm(f => ({
      ...f,
      items: f.items.map((item, i) => {
        if (i !== index) return item;
        const newVal = Math.max(1, (item[field] || 0) + delta);
        return { ...item, [field]: newVal };
      })
    }));
  };

  const getItemTotal = (item) => {
    const qty = parseInt(item.quantity) || 0;
    const tpd = parseInt(item.timesPerDay) || 0;
    const days = parseInt(item.durationDays) || 0;
    return qty * tpd * days;
  };

  const validate = () => {
    const e = {};
    if (!form.prescriptionNumber?.trim()) e.prescriptionNumber = 'Prescription number is required';
    if (!form.patientName?.trim()) e.patientName = 'Patient name is required';
    if (!form.items || form.items.length === 0) e.items = 'At least one medicine item is required';
    if (form.items) {
      form.items.forEach((item, i) => {
        if (!item.medicineId) e[`item_${i}_medicine`] = 'Medicine is required';
        if (!item.quantity || item.quantity <= 0) e[`item_${i}_quantity`] = 'Qty/dose must be > 0';
        if (!item.timesPerDay || item.timesPerDay <= 0) e[`item_${i}_timesPerDay`] = 'Times/day must be > 0';
        if (!item.durationDays || item.durationDays <= 0) e[`item_${i}_durationDays`] = 'Duration must be > 0';
      });
    }
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSave = async () => {
    if (!validate()) return;
    try {
      const payload = {
        ...form,
        items: (form.items || []).map(item => ({
          medicine: { id: parseInt(item.medicineId) },
          quantity: parseInt(item.quantity) || 1,
          dosage: item.dosage || '',
          timesPerDay: parseInt(item.timesPerDay) || 1,
          durationDays: parseInt(item.durationDays) || 1,
          notes: item.notes || ''
        }))
      };
      if (editing) { await prescriptionApi.update(editing.id, payload); toast.success('Prescription updated'); }
      else { await prescriptionApi.create(payload); toast.success('Prescription created & dispensed — awaiting cashier approval'); }
      setModalOpen(false); loadData();
    } catch (err) { toast.error(err.message); }
  };

  const handleDelete = async () => {
    try { await prescriptionApi.delete(confirmDelete.id); toast.success('Prescription deleted'); setConfirmDelete(null); loadData(); }
    catch (err) { toast.error(err.message); }
  };

  return (
    <>
      <div className="page-header">
        <div><h1>Prescriptions</h1><div className="page-header-subtitle">Medicines are dispensed automatically when a prescription is created</div></div>
        <button className="btn btn-primary" onClick={openCreate}>+ New Prescription</button>
      </div>
      <div className="page-body">
        <div className="toolbar">
          <div className="search-box">
            <span className="search-icon">🔍</span>
            <input placeholder="Search by patient, doctor, or Rx number..." value={search} onChange={(e) => setSearch(e.target.value)} />
          </div>
          <select className="form-select" style={{ width: 160 }} value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            <option value="">All Statuses</option>
            <option value="pending">Pending</option>
            <option value="dispensed">Dispensed</option>
            <option value="voided">Voided</option>
          </select>
        </div>
        <div className="card">
          <div className="table-wrapper">
            <table>
              <thead><tr><th>Rx #</th><th>Patient</th><th>Doctor</th><th>Hospital</th><th>Medicines</th><th>Status</th><th>Date</th><th>Actions</th></tr></thead>
              <tbody>
                {loading ? (
                  <tr><td colSpan="8"><div className="loading-spinner"><div className="spinner" /></div></td></tr>
                ) : filtered.length === 0 ? (
                  <tr><td colSpan="8"><div className="empty-state"><div className="icon">📝</div><h3>No prescriptions found</h3></div></td></tr>
                ) : filtered.map(p => (
                  <tr key={p.id}>
                    <td><strong>{p.prescriptionNumber}</strong></td>
                    <td>{p.patientName}</td>
                    <td>{p.doctorName || '-'}</td>
                    <td>{p.hospitalName || '-'}</td>
                    <td style={{ maxWidth: 280 }}>
                      {p.items && p.items.length > 0 ? (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                          {p.items.map((item, i) => {
                            const total = (item.quantity || 0) * (item.timesPerDay || 1) * (item.durationDays || 1);
                            return (
                              <span key={i} style={{ fontSize: 12 }}>
                                {item.medicine?.name || 'Unknown'} — {item.quantity}/dose × {item.timesPerDay}x/day × {item.durationDays}d = <strong>{total}</strong>
                                {item.dosage ? ` (${item.dosage})` : ''}
                              </span>
                            );
                          })}
                        </div>
                      ) : (
                        <span style={{ fontSize: 12, color: 'var(--text-secondary)' }}>No items</span>
                      )}
                    </td>
                    <td>
                      {p.voided
                        ? <span className="badge badge-red">Voided</span>
                        : p.dispensed
                          ? <span className="badge badge-green">Dispensed</span>
                          : <span className="badge badge-amber">Pending</span>}
                    </td>
                    <td>{p.createdAt ? new Date(p.createdAt).toLocaleDateString() : '-'}</td>
                    <td>
                      {!p.dispensed && !p.voided && (
                        <>
                          <button className="btn btn-ghost btn-sm" onClick={() => openEdit(p)}>Edit</button>
                          <button className="btn btn-ghost btn-sm" style={{ color: 'var(--danger)' }} onClick={() => setConfirmDelete(p)}>Delete</button>
                        </>
                      )}
                      {p.dispensed && !p.voided && (
                        <span style={{ color: 'var(--text-secondary)', fontSize: 12 }}>Dispensed — cashier action</span>
                      )}
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
          <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 720 }}>
            <div className="modal-header">
              <h2>{editing ? 'Edit Prescription' : 'New Prescription'}</h2>
              <button className="modal-close" onClick={() => setModalOpen(false)}>×</button>
            </div>
            <div className="modal-body">
              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">Prescription Number <span className="required">*</span></label>
                  <input className={`form-input ${errors.prescriptionNumber ? 'error' : ''}`} value={form.prescriptionNumber || ''} onChange={e => setForm({...form, prescriptionNumber: e.target.value})} />
                  {errors.prescriptionNumber && <div className="form-error">{errors.prescriptionNumber}</div>}
                </div>
                <div className="form-group">
                  <label className="form-label">Patient Name <span className="required">*</span></label>
                  <input className={`form-input ${errors.patientName ? 'error' : ''}`} value={form.patientName || ''} onChange={e => setForm({...form, patientName: e.target.value})} />
                  {errors.patientName && <div className="form-error">{errors.patientName}</div>}
                </div>
              </div>
              <div className="form-group">
                <label className="form-label">Patient ID Number</label>
                <input className="form-input" value={form.patientIdNumber || ''} onChange={e => setForm({...form, patientIdNumber: e.target.value})} />
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">Doctor Name</label>
                  <input className="form-input" value={form.doctorName || ''} onChange={e => setForm({...form, doctorName: e.target.value})} />
                </div>
                <div className="form-group">
                  <label className="form-label">Hospital Name</label>
                  <input className="form-input" value={form.hospitalName || ''} onChange={e => setForm({...form, hospitalName: e.target.value})} />
                </div>
              </div>

              <div style={{ borderTop: '1px solid var(--border)', marginTop: 16, paddingTop: 16 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
                  <label className="form-label" style={{ margin: 0, fontSize: 14, fontWeight: 600 }}>Medicine Items <span className="required">*</span></label>
                  <button className="btn btn-primary btn-sm" onClick={addItem} style={{ padding: '4px 12px', fontSize: 12 }}>+ Add Medicine</button>
                </div>
                {errors.items && <div className="form-error" style={{ marginBottom: 8 }}>{errors.items}</div>}
                {!editing && (
                  <div className="form-hint" style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 10 }}>
                    ⚡ Each item will be dispensed automatically from available stock when you create this prescription.
                  </div>
                )}

                {form.items && form.items.length > 0 ? (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                    {form.items.map((item, i) => {
                      const itemTotal = getItemTotal(item);
                      return (
                        <div key={i} style={{ border: '1px solid var(--border)', borderRadius: 8, padding: 12, position: 'relative' }}>
                          <button onClick={() => removeItem(i)} style={{ position: 'absolute', top: 8, right: 8, background: 'none', border: 'none', color: 'var(--danger)', cursor: 'pointer', fontSize: 16 }}>×</button>
                          <div style={{ marginBottom: 8 }}>
                            <label className="form-label" style={{ fontSize: 11 }}>Medicine</label>
                            <select className={`form-select ${errors[`item_${i}_medicine`] ? 'error' : ''}`} value={item.medicineId || ''} onChange={e => updateItem(i, 'medicineId', e.target.value)} style={{ fontSize: 13 }}>
                              <option value="">Select medicine</option>
                              {medicines.map(m => <option key={m.id} value={m.id}>{m.name} ({m.code})</option>)}
                            </select>
                            {errors[`item_${i}_medicine`] && <div className="form-error">{errors[`item_${i}_medicine`]}</div>}
                          </div>
                          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr 1fr', gap: 8, marginBottom: 8 }}>
                            <div>
                              <label className="form-label" style={{ fontSize: 11 }}>Dosage (text)</label>
                              <input className="form-input" placeholder="e.g. 500mg" value={item.dosage || ''} onChange={e => updateItem(i, 'dosage', e.target.value)} style={{ fontSize: 13 }} />
                            </div>
                            <div>
                              <label className="form-label" style={{ fontSize: 11 }}>Qty / Dose <span className="required">*</span></label>
                              <div style={{ display: 'flex', alignItems: 'center' }}>
                                <button onClick={() => adjustItemField(i, 'quantity', -1)} style={{ width: 28, height: 34, border: '1px solid var(--border)', borderRight: 'none', borderRadius: '6px 0 0 6px', background: 'var(--bg-secondary)', cursor: 'pointer' }}>-</button>
                                <input type="number" min="1" className={`form-input ${errors[`item_${i}_quantity`] ? 'error' : ''}`} value={item.quantity || ''} onChange={e => updateItem(i, 'quantity', parseInt(e.target.value) || '')} style={{ borderRadius: 0, textAlign: 'center', width: '100%', fontSize: 13 }} />
                                <button onClick={() => adjustItemField(i, 'quantity', 1)} style={{ width: 28, height: 34, border: '1px solid var(--border)', borderLeft: 'none', borderRadius: '0 6px 6px 0', background: 'var(--bg-secondary)', cursor: 'pointer' }}>+</button>
                              </div>
                              {errors[`item_${i}_quantity`] && <div className="form-error">{errors[`item_${i}_quantity`]}</div>}
                            </div>
                            <div>
                              <label className="form-label" style={{ fontSize: 11 }}>Times / Day <span className="required">*</span></label>
                              <div style={{ display: 'flex', alignItems: 'center' }}>
                                <button onClick={() => adjustItemField(i, 'timesPerDay', -1)} style={{ width: 28, height: 34, border: '1px solid var(--border)', borderRight: 'none', borderRadius: '6px 0 0 6px', background: 'var(--bg-secondary)', cursor: 'pointer' }}>-</button>
                                <input type="number" min="1" className={`form-input ${errors[`item_${i}_timesPerDay`] ? 'error' : ''}`} value={item.timesPerDay || ''} onChange={e => updateItem(i, 'timesPerDay', parseInt(e.target.value) || '')} style={{ borderRadius: 0, textAlign: 'center', width: '100%', fontSize: 13 }} />
                                <button onClick={() => adjustItemField(i, 'timesPerDay', 1)} style={{ width: 28, height: 34, border: '1px solid var(--border)', borderLeft: 'none', borderRadius: '0 6px 6px 0', background: 'var(--bg-secondary)', cursor: 'pointer' }}>+</button>
                              </div>
                              {errors[`item_${i}_timesPerDay`] && <div className="form-error">{errors[`item_${i}_timesPerDay`]}</div>}
                            </div>
                            <div>
                              <label className="form-label" style={{ fontSize: 11 }}>Duration (days) <span className="required">*</span></label>
                              <div style={{ display: 'flex', alignItems: 'center' }}>
                                <button onClick={() => adjustItemField(i, 'durationDays', -1)} style={{ width: 28, height: 34, border: '1px solid var(--border)', borderRight: 'none', borderRadius: '6px 0 0 6px', background: 'var(--bg-secondary)', cursor: 'pointer' }}>-</button>
                                <input type="number" min="1" className={`form-input ${errors[`item_${i}_durationDays`] ? 'error' : ''}`} value={item.durationDays || ''} onChange={e => updateItem(i, 'durationDays', parseInt(e.target.value) || '')} style={{ borderRadius: 0, textAlign: 'center', width: '100%', fontSize: 13 }} />
                                <button onClick={() => adjustItemField(i, 'durationDays', 1)} style={{ width: 28, height: 34, border: '1px solid var(--border)', borderLeft: 'none', borderRadius: '0 6px 6px 0', background: 'var(--bg-secondary)', cursor: 'pointer' }}>+</button>
                              </div>
                              {errors[`item_${i}_durationDays`] && <div className="form-error">{errors[`item_${i}_durationDays`]}</div>}
                            </div>
                          </div>
                          {itemTotal > 0 && (
                            <div style={{ fontSize: 12, color: 'var(--text-secondary)', background: 'var(--bg-secondary)', padding: '4px 10px', borderRadius: 4 }}>
                              Total: {item.quantity} × {item.timesPerDay} × {item.durationDays} = <strong style={{ color: 'var(--text-primary)' }}>{itemTotal} units</strong>
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                ) : (
                  <div style={{ textAlign: 'center', padding: 20, color: 'var(--text-secondary)', fontSize: 13, border: '1px dashed var(--border)', borderRadius: 8 }}>
                    No medicine items added yet. Click "+ Add Medicine" to start.
                  </div>
                )}
              </div>

              <div className="form-group" style={{ marginTop: 12 }}>
                <label className="form-label">Additional Notes</label>
                <textarea className="form-textarea" style={{ minHeight: 60 }} value={form.prescriptionDetails || ''} onChange={e => setForm({...form, prescriptionDetails: e.target.value})} placeholder="Any additional notes..." />
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn btn-secondary" onClick={() => setModalOpen(false)}>Cancel</button>
              <button className="btn btn-primary" onClick={handleSave}>{editing ? 'Update' : 'Create & Dispense'}</button>
            </div>
          </div>
        </div>
      )}

      <ConfirmDialog open={!!confirmDelete} title="Delete Prescription" message={`Delete prescription "${confirmDelete?.prescriptionNumber}"?`} onConfirm={handleDelete} onCancel={() => setConfirmDelete(null)} />
    </>
  );
}
