import { useEffect, useState } from "react";
import { useAuthStore } from "../auth/authStore";
import {
  gymScheduleApi,
  holidayApi,
  trainerScheduleApi,
} from "../api/schedules";
import { trainersApi } from "../api/administration";
import { errorMessage } from "../api/client";
import { MonthCalendar } from "../components/MonthCalendar";
import axios from "axios";
import type {
  GymSchedule,
  Holiday,
  TrainerProfile,
  TrainerSchedule,
} from "../types";
const days = [
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
  "SUNDAY",
];
const labels: Record<string, string> = {
  MONDAY: "Ponedeljak",
  TUESDAY: "Utorak",
  WEDNESDAY: "Sreda",
  THURSDAY: "Četvrtak",
  FRIDAY: "Petak",
  SATURDAY: "Subota",
  SUNDAY: "Nedelja",
  WORKING: "Radno vreme",
  HOLIDAY: "Praznik",
  SICK_LEAVE: "Bolovanje",
  VACATION: "Odmor",
};
export function SchedulesPage() {
  const own = useAuthStore((s) => s.session?.activeRole) === "TRAINER";
  const [gym, setGym] = useState<GymSchedule[]>([]);
  const [holidays, setHolidays] = useState<Holiday[]>([]);
  const [trainers, setTrainers] = useState<TrainerProfile[]>([]);
  const [trainerId, setTrainerId] = useState<number>();
  const [rows, setRows] = useState<TrainerSchedule[]>([]);
  const [notice, setNotice] = useState("");
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().slice(0, 10));
  const [recurring, setRecurring] = useState(false);
  const [overwrite, setOverwrite] = useState<{message:string;run:()=>Promise<void>}|null>(null);
  const [shift, setShift] = useState({
    date: new Date().toISOString().slice(0, 10),
    startTime: "08:00:00",
    endTime: "16:00:00",
  });
  const [away, setAway] = useState({
    startDate: new Date().toISOString().slice(0, 10),
    endDate: new Date().toISOString().slice(0, 10),
    status: "VACATION",
  });
  async function loadBase() {
    try {
      if (!own) {
        setGym(await gymScheduleApi.list());
        setHolidays(await holidayApi.list());
        const ts = await trainersApi.list();
        setTrainers(ts);
        if (!trainerId && ts[0]) setTrainerId(ts[0].id);
      }
    } catch (e) {
      setNotice(errorMessage(e));
    }
  }
  async function loadRows() {
    try {
      if (own || trainerId)
        setRows(await trainerScheduleApi.list(own ? undefined : trainerId));
    } catch (e) {
      setNotice(errorMessage(e));
    }
  }
  useEffect(() => {
    void loadBase();
  }, [own]);
  useEffect(() => {
    void loadRows();
  }, [own, trainerId]);
  async function addShift(e: React.FormEvent) {
    e.preventDefault();
    try {
      const request={ ...shift, ...(!own && { trainerId }) };
      if(recurring){const result=await trainerScheduleApi.recurring(request,own);setNotice(`Kreirano smena: ${result.createdCount}${result.skippedReasons.length?`. Preskočeno: ${result.skippedReasons.length}`:''}`)}
      else await trainerScheduleApi.create(request,own);
      await loadRows();
    } catch (x) {
      if(axios.isAxiosError(x)&&x.response?.data?.code==='SCHEDULE_OVERLAP_CONFIRMATION_REQUIRED'){
        setOverwrite({message:x.response.data.message,run:async()=>{await trainerScheduleApi.create({...shift,...(!own&&{trainerId}),confirmOverwrite:true},own);await loadRows()}});return
      }
      setNotice(errorMessage(x));
    }
  }
  async function addAway(e: React.FormEvent) {
    e.preventDefault();
    try {
      await trainerScheduleApi.unavailable(
        { ...away, ...(!own && { trainerId }) },
        own,
      );
      await loadRows();
    } catch (x) {
      if(axios.isAxiosError(x)&&x.response?.data?.code==='SCHEDULE_OVERLAP_CONFIRMATION_REQUIRED'){
        setOverwrite({message:x.response.data.message,run:async()=>{await trainerScheduleApi.unavailable({...away,...(!own&&{trainerId}),confirmOverwrite:true},own);await loadRows()}});return
      }
      setNotice(errorMessage(x));
    }
  }
  async function editShift(row: TrainerSchedule) {
    const startTime = prompt("Početak smene (HH:mm)", row.startTime.slice(0, 5));
    const endTime = prompt("Kraj smene (HH:mm)", row.endTime.slice(0, 5));
    if (!startTime || !endTime) return;
    try {
      await trainerScheduleApi.update(row.id, { trainerId: own ? undefined : trainerId, date: row.date, startTime: `${startTime}:00`, endTime: `${endTime}:00` }, own);
      await loadRows();
    } catch (x) { setNotice(errorMessage(x)); }
  }
  async function saveGym(
    day: string,
    startTime: string,
    endTime: string,
    id?: number,
  ) {
    try {
      await gymScheduleApi.save(
        {
          day,
          openingTime: startTime + ":00".replace(/:00:00$/, ":00"),
          closingTime: endTime + ":00".replace(/:00:00$/, ":00"),
        },
        id,
      );
      setGym(await gymScheduleApi.list());
    } catch (x) {
      setNotice(errorMessage(x));
    }
  }
  return (
    <main className="workspace-page schedule-page">
      <header className="workspace-header">
        <div>
          <p className="eyebrow">
            {own ? "Trainer / self-service" : "Manager / operacije"}
          </p>
          <h1>{own ? "Moj raspored" : "Rasporedi i neradni dani"}</h1>
          <p>
            {own
              ? "Upravljajte isključivo sopstvenim smenama i odsustvima."
              : "Radno vreme teretane, praznici i rasporedi svih trenera."}
          </p>
        </div>
      </header>
      {notice && (
        <div className="content-error">
          {notice}
          <button onClick={() => setNotice("")}>×</button>
        </div>
      )}
      {overwrite&&<div className="overwrite-backdrop"><section className="progress-card overwrite-confirm"><p className="eyebrow">Potvrda zamene</p><h2>Preklapanje rasporeda</h2><p>{overwrite.message}</p><div><button className="secondary-button" onClick={()=>setOverwrite(null)}>Odustani</button><button className="primary-button" onClick={async()=>{try{await overwrite.run();setOverwrite(null)}catch(reason){setNotice(errorMessage(reason));setOverwrite(null)}}}>Zameni postojeći unos</button></div></section></div>}
      {!own && (
        <div className="schedule-admin-grid">
          <section className="progress-card">
            <div className="card-head">
              <div>
                <p className="eyebrow">Nedeljni ritam</p>
                <h2>Radno vreme teretane</h2>
              </div>
            </div>
            <div className="hours-list">
              {days.map((day) => {
                const row = gym.find((x) => x.day === day);
                return (
                  <form
                    key={day}
                    onSubmit={(e) => {
                      e.preventDefault();
                      const f = new FormData(e.currentTarget);
                      void saveGym(
                        day,
                        String(f.get("start")),
                        String(f.get("end")),
                        row?.id,
                      );
                    }}
                  >
                    <strong>{labels[day]}</strong>
                    <input
                      name="start"
                      type="time"
                      defaultValue={row?.openingTime?.slice(0, 5) ?? "07:00"}
                    />
                    <span>—</span>
                    <input
                      name="end"
                      type="time"
                      defaultValue={row?.closingTime?.slice(0, 5) ?? "22:00"}
                    />
                    <button>Sačuvaj</button>
                  </form>
                );
              })}
            </div>
          </section>
          <section className="progress-card">
            <div className="card-head">
              <div>
                <p className="eyebrow">Kalendar</p>
                <h2>Praznici</h2>
              </div>
            </div>
            <form
              className="holiday-form"
              onSubmit={async (e) => {
                e.preventDefault();
                const f = new FormData(e.currentTarget);
                await holidayApi.create({
                  date: String(f.get("date")),
                  description: String(f.get("description")),
                });
                setHolidays(await holidayApi.list());
                e.currentTarget.reset();
              }}
            >
              <input required name="date" type="date" />
              <input required name="description" placeholder="Opis praznika" />
              <button>Dodaj</button>
            </form>
            <div className="holiday-list">
              {holidays.map((h) => (
                <article key={h.id}>
                  <div>
                    <strong>
                      {new Date(h.date + "T12:00").toLocaleDateString("sr-Latn-RS")}
                    </strong>
                    <small>{h.description}</small>
                  </div>
                  <button
                    onClick={async () => {
                      await holidayApi.remove(h.id);
                      setHolidays(await holidayApi.list());
                    }}
                  >
                    ×
                  </button>
                </article>
              ))}
            </div>
          </section>
        </div>
      )}
      <section className="progress-card trainer-schedule-card">
        <div className="card-head">
          <div>
            <p className="eyebrow">
              {own ? "Lični kalendar" : "Nadzor trenera"}
            </p>
            <h2>
              {own ? "Moje smene i odsustva" : "Raspored izabranog trenera"}
            </h2>
          </div>
          {!own && (
            <select
              className="client-picker"
              value={trainerId ?? ""}
              onChange={(e) => setTrainerId(+e.target.value)}
            >
              {trainers.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.user.email}
                </option>
              ))}
            </select>
          )}
        </div>
        <div className="shift-controls">
          <form onSubmit={addShift}>
            <h3>Nova smena</h3>
            <input
              required
              type="date"
              value={shift.date}
              onChange={(e) => setShift({ ...shift, date: e.target.value })}
            />
            <input
              required
              type="time"
              value={shift.startTime.slice(0, 5)}
              onChange={(e) =>
                setShift({ ...shift, startTime: e.target.value + ":00" })
              }
            />
            <input
              required
              type="time"
              value={shift.endTime.slice(0, 5)}
              onChange={(e) =>
                setShift({ ...shift, endTime: e.target.value + ":00" })
              }
            />
            <button className="primary-button compact">Dodaj smenu</button>
            <label><input type="checkbox" checked={recurring} onChange={e=>setRecurring(e.target.checked)}/> Fiksni raspored (8 nedelja)</label>
          </form>
          <form onSubmit={addAway}>
            <h3>Neradni period</h3>
            <input
              required
              type="date"
              value={away.startDate}
              onChange={(e) => setAway({ ...away, startDate: e.target.value })}
            />
            <input
              required
              type="date"
              value={away.endDate}
              onChange={(e) => setAway({ ...away, endDate: e.target.value })}
            />
            <select
              value={away.status}
              onChange={(e) => setAway({ ...away, status: e.target.value })}
            >
              <option value="VACATION">Odmor</option>
              <option value="SICK_LEAVE">Bolovanje</option>
              <option value="HOLIDAY">Praznik</option>
            </select>
            <button className="secondary-button">Dodaj odsustvo</button>
          </form>
        </div>
        <div className={own?"calendar-list-layout":""}>{own&&<MonthCalendar value={selectedDate} onChange={setSelectedDate} highlightedDates={new Set(rows.map(row=>row.date))}/>}<div className="schedule-list">
          {rows.filter(row=>!own||row.date===selectedDate).map((r) => (
            <article key={r.id}>
              <time>
                {new Date(r.date + "T12:00").toLocaleDateString("sr-Latn-RS", {
                  weekday: "short",
                  day: "2-digit",
                  month: "short",
                })}
              </time>
              <div>
                <strong>{labels[r.status]}</strong>
                <small>
                  {r.startTime.slice(0, 5)} — {r.endTime.slice(0, 5)}
                </small>
              </div>
              <div className="row-actions">{r.status === "WORKING" && <button onClick={() => void editShift(r)}>Izmeni</button>}<button className="icon-danger" onClick={async () => { await trainerScheduleApi.remove(r.id, own); await loadRows(); }}>×</button></div>
            </article>
          ))}
          {!rows.length && (
            <div className="empty-panel">Još nema unetih termina.</div>
          )}
        </div></div>
      </section>
    </main>
  );
}
