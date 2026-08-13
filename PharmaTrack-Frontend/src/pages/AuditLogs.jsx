/**
 * Audit Logs Page
 * View-only page for system audit trail with filtering.
 */
import { useState, useEffect } from 'react';
import { auditLogApi } from '../api/api';
import { useToast } from '../components/Toast';

const ACTIONS = ['CREATE', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT', 'DISPENSE', 'STOCK_IN', 'STOCK_OUT'];
const ACTION_COLORS = {
  CREATE: 'green', UPDATE: 'amber', DELETE: 'red', LOGIN: 'teal',
  LOGOUT: 'gray', DISPENSE: 'indigo', STOCK_IN: 'green', STOCK_OUT: 'red'
};

export default function AuditLogs() {
  const toast = useToast();
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [actionFilter, setActionFilter] = useState('');

  const loadData = async () => {
    try {
      setLoading(true);
      const data = actionFilter ? await auditLogApi.getByAction(actionFilter) : await auditLogApi.getAll();
      setLogs(data);
    } catch (err) { toast.error(err.message); }
    finally { setLoading(false); }
  };

  useEffect(() => { loadData(); }, [actionFilter]);

  const filtered = logs.filter(l =>
    l.entityType?.toLowerCase().includes(search.toLowerCase()) ||
    l.ipAddress?.includes(search)
  );

  return (
    <>
      <div className="page-header">
        <div><h1>Audit Logs</h1><div className="page-header-subtitle">System activity audit trail</div></div>
      </div>
      <div className="page-body">
        <div className="toolbar">
          <div className="search-box">
            <span className="search-icon">🔍</span>
            <input placeholder="Search by entity type or IP..." value={search} onChange={(e) => setSearch(e.target.value)} />
          </div>
          <select className="form-select" style={{ width: 160 }} value={actionFilter} onChange={(e) => setActionFilter(e.target.value)}>
            <option value="">All Actions</option>
            {ACTIONS.map(a => <option key={a} value={a}>{a}</option>)}
          </select>
        </div>
        <div className="card">
          <div className="table-wrapper">
            <table>
              <thead><tr><th>ID</th><th>Action</th><th>Entity Type</th><th>Entity ID</th><th>By</th><th>IP Address</th><th>Timestamp</th></tr></thead>
              <tbody>
                {loading ? (
                  <tr><td colSpan="7"><div className="loading-spinner"><div className="spinner" /></div></td></tr>
                ) : filtered.length === 0 ? (
                  <tr><td colSpan="7"><div className="empty-state"><div className="icon">📜</div><h3>No audit logs found</h3></div></td></tr>
                ) : filtered.map(l => (
                  <tr key={l.id}>
                    <td>{l.id}</td>
                    <td><span className={`badge badge-${ACTION_COLORS[l.action] || 'gray'}`}>{l.action}</span></td>
                    <td>{l.entityType}</td>
                    <td>{l.entityId}</td>
                    <td>{l.performedBy?.fullName || '-'}</td>
                    <td><code style={{ fontSize: 12 }}>{l.ipAddress}</code></td>
                    <td>{l.timestamp ? new Date(l.timestamp).toLocaleString() : '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </>
  );
}
