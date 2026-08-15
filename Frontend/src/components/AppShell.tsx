import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuthStore } from "../auth/authStore";
import { decodeJwt } from "../auth/token";
import type { Role } from "../types";
import { NotificationCenter } from "./NotificationCenter";
const roleLabels: Record<Role, string> = {
  MANAGER: "Menadžer",
  TRAINER: "Trener",
  CLIENT: "Klijent",
  ADMIN: "Administrator",
};
export function AppShell() {
  const session = useAuthStore((s) => s.session)!;
  const setActiveRole = useAuthStore((s) => s.setActiveRole);
  const clear = useAuthStore((s) => s.clear);
  const navigate = useNavigate();
  const claims = decodeJwt(session.accessToken);
  const roles = (claims.roles ?? []).filter((role) => role !== "ADMIN");
  const manager = session.activeRole === "MANAGER";
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <span>FM</span>
          <div>
            Fitness<small>GymOS</small>
          </div>
        </div>
        <nav aria-label="Glavna navigacija">
          {manager ? (
            <>
              <p>Upravljanje</p>
              <NavLink to="/app/live">
                <i>◉</i> Plan uživo
              </NavLink>
              <NavLink to="/app/editor">
                <i>⌗</i> Editor sala
              </NavLink>
              <NavLink to="/app/insights">
                <i>✦</i> AI uvidi
              </NavLink>
              <p>Operacije</p>
              <NavLink to="/app/administration">
                <i>◎</i> Administracija
              </NavLink>
              <NavLink to="/app/schedules">
                <i>▦</i> Rasporedi
              </NavLink>
              <NavLink to="/app/calendar">
                <i>◫</i> Dnevni raspored
              </NavLink>
              <NavLink to="/app/manage-appointments">
                <i>＋</i> Upravljanje terminima
              </NavLink>
              <NavLink to="/app/payments">
                <i>¤</i> Plaćanja
              </NavLink>
            </>
          ) : (
            <>
              <NavLink to="/app/progress">
                <i>↗</i> Praćenje napretka
              </NavLink>
              {session.activeRole === "TRAINER" && (
                <><NavLink to="/app/live"><i>◉</i> Plan uživo</NavLink><NavLink to="/app/appointments"><i>◫</i> Moji termini</NavLink><NavLink to="/app/schedules"><i>▦</i> Moj raspored</NavLink></>
              )}
              {session.activeRole === "CLIENT" && (
                <><NavLink to="/app/appointments"><i>◫</i> Zakaži trening</NavLink><NavLink to="/app/payments"><i>¤</i> Moje uplate</NavLink></>
              )}
            </>
          )}
        </nav>
        <div className="sidebar-bottom">
          <NotificationCenter />
          {roles.length > 1 && (
            <label>
              Aktivna oblast
              <select
                value={session.activeRole}
                onChange={(e) => {
                  setActiveRole(e.target.value as Role);
                  navigate("/app");
                }}
              >
                {roles.map((r) => (
                  <option key={r} value={r}>
                    {roleLabels[r]}
                  </option>
                ))}
              </select>
            </label>
          )}
          <div className="profile-chip">
            <span>{claims.email?.slice(0, 2).toUpperCase() ?? "FM"}</span>
            <div>
              <strong>{roleLabels[session.activeRole]}</strong>
              <small>{claims.email}</small>
            </div>
            <button
              aria-label="Odjavi se"
              title="Odjavi se"
              onClick={() => {
                clear();
                navigate("/login");
              }}
            >
              ↪
            </button>
          </div>
        </div>
      </aside>
      <div className="main-column">
        <Outlet />
      </div>
    </div>
  );
}
