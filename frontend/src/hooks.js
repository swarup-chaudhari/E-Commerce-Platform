import { useCallback, useEffect, useRef, useState } from 'react';

/** Loads data with loading/error state; optionally re-polls while shouldPoll(data) is true. */
export function useAsync(fn, deps, { pollMs = 0, shouldPoll = () => false } = {}) {
  const [state, setState] = useState({ data: null, error: null, loading: true });
  const fnRef = useRef(fn);
  fnRef.current = fn;

  const load = useCallback(async (silent) => {
    if (!silent) setState((s) => ({ ...s, loading: true }));
    try {
      const data = await fnRef.current();
      setState({ data, error: null, loading: false });
      return data;
    } catch (error) {
      setState((s) => ({ ...s, error, loading: false }));
      return null;
    }
  }, []);

  useEffect(() => {
    let cancelled = false;
    let timer;
    const tick = async (silent) => {
      const data = await load(silent);
      if (cancelled) return;
      if (pollMs && data && shouldPoll(data)) timer = setTimeout(() => tick(true), pollMs);
    };
    tick(false);
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  return { ...state, reload: () => load(true) };
}
