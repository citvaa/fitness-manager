import { useEffect, useState } from "react";
import { clientsApi, trainersApi, usersApi } from "../api/administration";
import { API_URL, errorMessage } from "../api/client";
import { useConfirm } from "../components/ConfirmDialog";
import type {
  ClientProfile,
  EmploymentStatus,
  Role,
  TrainerProfile,
  UserAccount,
} from "../types";
const roles: Role[] = ["MANAGER", "TRAINER", "CLIENT"];
const roleLabel: Record<Role, string> = {
  MANAGER: "Menadžer",
  TRAINER: "Trener",
  CLIENT: "Klijent",
  ADMIN: "Administrator",
};
export function AdministrationPage() {
  const { requestConfirmation, confirmationDialog } = useConfirm();
  const [tab, setTab] = useState<"users" | "trainers" | "clients">("users");
  const [users, setUsers] = useState<UserAccount[]>([]);
  const [trainers, setTrainers] = useState<TrainerProfile[]>([]);
  const [clients, setClients] = useState<ClientProfile[]>([]);
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [pages, setPages] = useState(1);
  const [email, setEmail] = useState("");
  const [initialRoles, setInitialRoles] = useState<Role[]>([]);
  const [profile, setProfile] = useState({
    employmentDate: new Date().toISOString().slice(0, 10),
    birthYear: 1990,
    status: "FULL_TIME" as EmploymentStatus,
  });
  const [invite, setInvite] = useState("");
  const [notice, setNotice] = useState("");
  const [emailEdit, setEmailEdit] = useState<{ item: UserAccount | TrainerProfile | ClientProfile; value: string } | null>(null);
  async function load() {
    try {
      if (tab === "users") {
        const d = await usersApi.list(search, page);
        setUsers(d.content);
        setPages(Math.max(1, d.totalPages));
      }
      if (tab === "trainers") setTrainers(await trainersApi.list());
      if (tab === "clients") setClients(await clientsApi.list());
    } catch (e) {
      setNotice(errorMessage(e));
    }
  }
  useEffect(() => {
    void load();
  }, [tab, page]);
  async function create(e: React.FormEvent) {
    e.preventDefault();
    try {
      if (tab === "users") {
        const u = await usersApi.create(email);
        for (const role of initialRoles) await usersApi.addRole(u.id, role);
        setInvite(
          `${location.origin}/complete-registration?key=${u.registrationKey}`,
        );
      } else if (tab === "trainers") {
        const t = await trainersApi.create({ email, ...profile });
        setInvite(
          `${location.origin}/complete-registration?key=${t.user.registrationKey}`,
        );
      } else {
        const c = await clientsApi.create(email);
        setInvite(
          `${location.origin}/complete-registration?key=${c.user.registrationKey}`,
        );
      }
      setEmail("");
      await load();
    } catch (x) {
      setNotice(errorMessage(x));
    }
  }
  async function toggleRole(u: UserAccount, r: Role) {
    try {
      u.roles.includes(r)
        ? await usersApi.removeRole(u.id, r)
        : await usersApi.addRole(u.id, r);
      await load();
    } catch (x) {
      setNotice(errorMessage(x));
    }
  }
  async function remove(id: number) {
    if (!await requestConfirmation({ title: "Brisanje zapisa", message: "Obrisati izabrani zapis?", confirmLabel: "Obriši" })) return;
    try {
      tab === "users"
        ? await usersApi.remove(id)
        : tab === "trainers"
          ? await trainersApi.remove(id)
          : await clientsApi.remove(id);
      await load();
    } catch (x) {
      setNotice(errorMessage(x));
    }
  }
  function edit(item: UserAccount | TrainerProfile | ClientProfile) {
    const current = "email" in item ? item.email : item.user.email;
    setEmailEdit({ item, value: current });
  }
  async function saveEmail(e: React.FormEvent) {
    e.preventDefault();
    if (!emailEdit) return;
    const { item, value: next } = emailEdit;
    try {
      if ("email" in item) await usersApi.update(item.id, next);
      else if ("employmentDate" in item) {
        const trainer = item as TrainerProfile;
        await trainersApi.update(trainer.id, { email: next, employmentDate: trainer.employmentDate, birthYear: trainer.birthYear, status: trainer.status });
      } else await clientsApi.update(item.id, next);
      setEmailEdit(null);
      setNotice("Email adresa je sačuvana.");
      await load();
    } catch (x) { setNotice(errorMessage(x)); }
  }
  return (
    <main className="workspace-page admin-page">
      {confirmationDialog}
      <header className="workspace-header">
        <div>
          <p className="eyebrow">Manager / administracija</p>
          <h1>Ljudi i pristup</h1>
          <p>
            Nalozi i domenski profili ostaju jasno odvojeni, ali povezani email
            adresom.
          </p>
        </div>
      </header>
      <div className="tabs">
        {(["users", "trainers", "clients"] as const).map((x) => (
          <button
            className={tab === x ? "active" : ""}
            onClick={() => {
              setTab(x);
              setPage(0);
            }}
            key={x}
          >
            {x === "users"
              ? "Korisnici"
              : x === "trainers"
                ? "Treneri"
                : "Klijenti"}
          </button>
        ))}
      </div>
      {notice && (
        <div className="content-error">
          {notice}
          <button onClick={() => setNotice("")}>×</button>
        </div>
      )}
      <section className="admin-grid">
        <form className="progress-card admin-form" onSubmit={create}>
          <p className="eyebrow">Novi zapis</p>
          <h2>
            {tab === "users"
              ? "Korisnički nalog"
              : tab === "trainers"
                ? "Profil trenera"
                : "Profil klijenta"}
          </h2>
          <label>
            Email
            <input
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </label>
          {tab === "users" && <div className="initial-roles"><small>Inicijalne role</small>{roles.map((role) => <label key={role}><input type="checkbox" checked={initialRoles.includes(role)} onChange={() => setInitialRoles((value) => value.includes(role) ? value.filter((item) => item !== role) : [...value, role])}/>{roleLabel[role]}</label>)}</div>}
          {tab === "trainers" && (
            <>
              <label>
                Datum zaposlenja
                <input
                  type="date"
                  required
                  value={profile.employmentDate}
                  onChange={(e) =>
                    setProfile({ ...profile, employmentDate: e.target.value })
                  }
                />
              </label>
              <label>
                Godina rođenja
                <input
                  type="number"
                  required
                  value={profile.birthYear}
                  onChange={(e) =>
                    setProfile({ ...profile, birthYear: +e.target.value })
                  }
                />
              </label>
              <label>
                Status
                <select
                  value={profile.status}
                  onChange={(e) =>
                    setProfile({
                      ...profile,
                      status: e.target.value as EmploymentStatus,
                    })
                  }
                >
                  <option value="FULL_TIME">Stalni radni odnos</option>
                  <option value="CONTRACT">Ugovor</option>
                  <option value="FORMER_EMPLOYEE">Bivši zaposleni</option>
                </select>
              </label>
            </>
          )}
          <button className="primary-button">
            Kreiraj i pripremi aktivaciju
          </button>
        </form>
        <section className="progress-card table-card">
          <div className="card-head">
            <div>
              <p className="eyebrow">Pregled</p>
              <h2>
                {tab === "users"
                  ? "Svi korisnici"
                  : tab === "trainers"
                    ? "Profili trenera"
                    : "Profili klijenata"}
              </h2>
            </div>
            {tab === "users" && (
              <form
                onSubmit={(e) => {
                  e.preventDefault();
                  setPage(0);
                  void load();
                }}
                className="search-form"
              >
                <input
                  placeholder="Pretraži email…"
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                />
                <button>Traži</button>
              </form>
            )}
          </div>
          <div className="data-table">
            {tab === "users" &&
              users.map((u) => (
                <article key={u.id}>
                  <div>
                    <strong>{u.email}</strong>
                    <small>
                      {u.isActivated ? "Aktivan" : "Čeka aktivaciju"} · #{u.id}
                    </small>
                  </div>
                  <div className="role-pills">
                    {roles.map((r) => (
                      <button
                        key={r}
                        className={u.roles.includes(r) ? "on" : ""}
                        onClick={() => void toggleRole(u, r)}
                      >
                        {roleLabel[r]}
                      </button>
                    ))}
                  </div>
                  <div className="row-actions"><button onClick={() => void edit(u)}>Izmeni</button><button className="icon-danger" onClick={() => void remove(u.id)}>×</button></div>
                </article>
              ))}
            {tab === "trainers" &&
              trainers.map((t) => (
                <article key={t.id}>
                  <div>
                    <strong>{t.user.email}</strong>
                    <small>
                      Trener #{t.id} · {t.status} · {t.birthYear}
                    </small>
                  </div>
                  <span className="status-chip">
                    {t.user.isActivated ? "Aktivan" : "Pozvan"}
                  </span>
                  <div className="row-actions"><button onClick={() => void edit(t)}>Izmeni</button><button className="icon-danger" onClick={() => void remove(t.id)}>×</button></div>
                </article>
              ))}
            {tab === "clients" &&
              clients.map((c) => (
                <article key={c.id}>
                  <div>
                    <strong>{c.user.email}</strong>
                    <small>
                      Klijent #{c.id} · User #{c.user.id}
                    </small>
                  </div>
                  <span className="status-chip">
                    {c.user.isActivated ? "Aktivan" : "Pozvan"}
                  </span>
                  <div className="row-actions"><button onClick={() => void edit(c)}>Izmeni</button><button className="icon-danger" onClick={() => void remove(c.id)}>×</button></div>
                </article>
              ))}
          </div>
          {tab === "users" && (
            <div className="pagination">
              <button
                disabled={page === 0}
                onClick={() => setPage((p) => p - 1)}
              >
                ←
              </button>
              <span>
                {page + 1} / {pages}
              </span>
              <button
                disabled={page + 1 >= pages}
                onClick={() => setPage((p) => p + 1)}
              >
                →
              </button>
            </div>
          )}
        </section>
      </section>
      {emailEdit && (
        <div className="modal-backdrop" onMouseDown={() => setEmailEdit(null)}>
          <section className="modal" onMouseDown={(event) => event.stopPropagation()}>
            <form onSubmit={saveEmail}>
              <div className="modal-head">
                <div><p className="eyebrow">Izmena naloga</p><h2>Promena email adrese</h2></div>
                <button type="button" onClick={() => setEmailEdit(null)}>×</button>
              </div>
              <div className="form-grid">
                <label>Email<input autoFocus required type="email" value={emailEdit.value} onChange={(event) => setEmailEdit({ ...emailEdit, value: event.target.value })} /></label>
              </div>
              <button className="primary-button">Sačuvaj email</button>
            </form>
          </section>
        </div>
      )}
      {invite && (
        <div className="modal-backdrop">
          <div className="modal invite-modal">
            <div className="modal-head">
              <div>
                <p className="eyebrow">Dev / demo način</p>
                <h2>Aktivacioni link je spreman</h2>
              </div>
              <button onClick={() => setInvite("")}>×</button>
            </div>
            <p>
              U produkciji se ovaj link dostavlja{" "}
              <strong>isključivo emailom</strong>. Prikazan je ovde zato što
              demo okruženje nema stvarne SMTP kredencijale.
            </p>
            <code>{invite}</code>
            <div className="invite-actions">
              <button
                className="secondary-button"
                onClick={() => void navigator.clipboard.writeText(invite)}
              >
                Kopiraj link
              </button>
              <a
                className="primary-button compact"
                href={invite}
                target="_blank"
              >
                Otvori aktivaciju ↗
              </a>
            </div>
            <small>Backend: {API_URL}</small>
          </div>
        </div>
      )}
    </main>
  );
}
