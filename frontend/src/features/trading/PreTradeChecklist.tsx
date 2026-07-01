import { useEffect, useState } from "react";
import { api, type SkillDto } from "../../lib/api";

interface ChecklistItem {
  label: string;
  checked: boolean;
  rule?: string;
}

/**
 * PreTradeChecklist — validates setup quality against ingested trading rules.
 * Queries skills with tag "trading" and builds a checklist from them.
 */
export default function PreTradeChecklist({ 
  instrument, 
  onComplete 
}: { 
  instrument: string; 
  onComplete?: (passed: boolean) => void;
}) {
  const [rules, setRules] = useState<SkillDto[]>([]);
  const [checklist, setChecklist] = useState<ChecklistItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [allPassed, setAllPassed] = useState(false);

  useEffect(() => {
    async function loadRules() {
      setLoading(true);
      try {
        // Fetch all skills tagged with "trading"
        const allSkills = await api.listSkills();
        const tradingRules = allSkills.filter(s => 
          s.tags?.toLowerCase().includes("trading") && 
          s.status === "active"
        );
        
        setRules(tradingRules);
        
        // Build checklist from rules or use defaults
        if (tradingRules.length > 0) {
          const items = tradingRules.map(rule => ({
            label: rule.name,
            checked: false,
            rule: rule.description || rule.content.slice(0, 100),
          }));
          setChecklist(items);
        } else {
          // Default checklist when no rules ingested
          setChecklist([
            { label: "Trend aligned", checked: false },
            { label: "Entry signal confirmed", checked: false },
            { label: "Stop-loss set", checked: false },
            { label: "Risk < 2% of capital", checked: false },
            { label: "Not revenge trading", checked: false },
          ]);
        }
      } catch (e) {
        console.error("Failed to load trading rules:", e);
        // Fallback to defaults
        setChecklist([
          { label: "Trend aligned", checked: false },
          { label: "Entry signal confirmed", checked: false },
          { label: "Stop-loss set", checked: false },
          { label: "Risk < 2% of capital", checked: false },
          { label: "Not revenge trading", checked: false },
        ]);
      } finally {
        setLoading(false);
      }
    }
    
    loadRules();
  }, [instrument]);

  useEffect(() => {
    const passed = checklist.length > 0 && checklist.every(item => item.checked);
    setAllPassed(passed);
    if (onComplete) onComplete(passed);
  }, [checklist, onComplete]);

  function toggleItem(index: number) {
    setChecklist(prev => prev.map((item, i) => 
      i === index ? { ...item, checked: !item.checked } : item
    ));
  }

  if (loading) {
    return (
      <div className="rounded-lg bg-ultron-panel p-4">
        <h3 className="mb-3 font-mono text-sm uppercase tracking-widest text-ultron-accent">
          Pre-Trade Checklist
        </h3>
        <p className="text-xs text-gray-500">Loading rules...</p>
      </div>
    );
  }

  return (
    <div className="rounded-lg bg-ultron-panel p-4">
      <div className="mb-3 flex items-center justify-between">
        <h3 className="font-mono text-sm uppercase tracking-widest text-ultron-accent">
          Pre-Trade Checklist
        </h3>
        {rules.length > 0 && (
          <span className="text-[10px] text-gray-500">
            {rules.length} rule{rules.length !== 1 ? 's' : ''} loaded
          </span>
        )}
      </div>

      {rules.length === 0 && (
        <div className="mb-3 rounded border border-amber-500/30 bg-amber-950/20 p-2">
          <p className="text-[10px] text-amber-200">
            No trading rules ingested. Using default checklist. 
            To add custom rules: go to <span className="font-mono text-amber-300">SKILLS</span> tab and 
            ingest rules with tag "<span className="font-mono text-amber-300">trading</span>".
          </p>
        </div>
      )}

      <ul className="space-y-2">
        {checklist.map((item, i) => (
          <li key={i}>
            <label className="flex cursor-pointer items-start gap-2 rounded p-2 transition-colors hover:bg-black/20">
              <input
                type="checkbox"
                checked={item.checked}
                onChange={() => toggleItem(i)}
                className="mt-0.5 h-4 w-4 cursor-pointer accent-emerald-500"
              />
              <div className="flex-1">
                <span className={`text-sm ${item.checked ? 'text-gray-200' : 'text-gray-400'}`}>
                  {item.label}
                </span>
                {item.rule && (
                  <p className="mt-0.5 text-[10px] text-gray-500 leading-tight">
                    {item.rule}
                  </p>
                )}
              </div>
            </label>
          </li>
        ))}
      </ul>

      <div className={`mt-4 rounded border p-2 text-center text-sm ${
        allPassed 
          ? 'border-emerald-500/50 bg-emerald-950/30 text-emerald-400' 
          : 'border-gray-700 bg-gray-900/30 text-gray-500'
      }`}>
        {allPassed ? '✓ Ready to trade' : 'Complete checklist before trading'}
      </div>
    </div>
  );
}
