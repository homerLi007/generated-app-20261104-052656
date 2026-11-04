import { useState } from 'react';

export default function App() {
  const [input, setInput] = useState('');
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError(null);
    setResult(null);
    const paths = input.split('\n').map(s => s.trim()).filter(Boolean);
    if (paths.length === 0) {
      setError('Enter at least one path');
      return;
    }
    setLoading(true);
    try {
      const res = await fetch('/api/delete', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ paths })
      });
      const data = await res.json();
      if (!res.ok) {
        setError(data.error || 'Request failed');
      } else {
        setResult(data);
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="container">
      <h1>Safe File Deleter</h1>
      <p className="hint">Paths are relative to <code>allowed_root</code>. Symlinks and non-regular files are refused.</p>
      <form onSubmit={handleSubmit}>
        <textarea
          value={input}
          onChange={e => setInput(e.target.value)}
          placeholder={'file1.txt\nsubdir/file2.txt'}
          rows={6}
        />
        <button type="submit" disabled={loading}>{loading ? 'Deleting...' : 'Delete'}</button>
      </form>
      {error && <div className="error">{error}</div>}
      {result && (
        <div className="result">
          <h2>Deleted ({result.deleted.length})</h2>
          <ul>{result.deleted.map(p => <li key={p}>{p}</li>)}</ul>
          <h2>Rejected ({result.rejected.length})</h2>
          <ul>{result.rejected.map((r, i) => <li key={i}><code>{r.path}</code> — {r.reason}</li>)}</ul>
        </div>
      )}
    </div>
  );
}
