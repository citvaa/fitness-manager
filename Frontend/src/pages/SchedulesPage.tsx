import { useEffect, useState } from "react";
import { useAuthStore } from "../auth/authStore";
import {
  gymScheduleApi,
  holidayApi,
  trainerScheduleApi,
} from "../api/schedules";
import { errorMessage } from "../api/client";
import { MonthCalendar } from "../components/MonthCalendar";
import axios from "axios";
import type {
  GymSchedule,
  Holiday,
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
  const [rows, setRows] = useState<TrainerSchedule[]>([]);
  const [notice, setNotice] = useState("");
  const [savedDay, setSavedDay] = useState("");
  const [holidayForm, setHolidayForm] = useState({ date: new Date().toISOString().slice(0, 10), description: "" });
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
      setHolidays(await holidayApi.list());
      if (!own) {
        setGym(await gymScheduleApi.list());
      }
    } catch (e) {
      setNotice(errorMessage(e));
    }
  }
  async function loadRows() {
    try {
      if (own) setRows(await trainerScheduleApi.list());
    } catch (e) {
      setNotice(errorMessage(e));
    }
  }
  useEffect(() => {
    void loadBase();
  }, [own]);
  useEffect(() => {
    void loadRows();
  }, [own]);
  async function addShift(e: React.FormEvent) {
    e.preventDefault();
    try {
      if(recurring){const result=await trainerScheduleApi.recurring(shift,true);setNotice(`Kreirano smena: ${result.createdCount}${result.skippedReasons.length?`. Preskočeno: ${result.skippedReasons.length}`:''}`)}
      else await trainerScheduleApi.create(shift,true);
      await loadRows();
    } catch (x) {
      if(axios.isAxiosError(x)&&x.response?.data?.code==='SCHEDULE_OVERLAP_CONFIRMATION_REQUIRED'){
        setOverwrite({message:x.response.data.message,run:async()=>{await trainerScheduleApi.create({...shift,confirmOverwrite:true},true);await loadRows()}});return
      }
      setNotice(errorMessage(x));
    }
  }
  async function addAway(e: React.FormEvent) {
    e.preventDefault();
    try {
      await trainerScheduleApi.unavailable(
        away,
        true,
      );
      await loadRows();
    } catch (x) {
      if(axios.isAxiosError(x)&&x.response?.data?.code==='SCHEDULE_OVERLAP_CONFIRMATION_REQUIRED'){
        setOverwrite({message:x.response.data.message,run:async()=>{await trainerScheduleApi.unavailable({...away,confirmOverwrite:true},true);await loadRows()}});return
      }
      setNotice(errorMessage(x));
    }
  }
  async function editShift(row: TrainerSchedule) {
    const startTime = prompt("Početak smene (HH:mm)", row.startTime.slice(0, 5));
    const endTime = prompt("Kraj smene (HH:mm)", row.endTime.slice(0, 5));
    if (!startTime || !endTime) return;
    try {
      await trainerScheduleApi.update(row.id, { date: row.date, startTime: `${startTime}:00`, endTime: `${endTime}:00` }, true);
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
      setSavedDay(day);
      window.setTimeout(() => setSavedDay(current => current === day ? "" : current), 1800);
    } catch (x) {
      setNotice(errorMessage(x));
    }
  }
  const unavailableReasons:Record<TrainerSchedule['status'],string>={
    WORKING:"",
    HOLIDAY:"Trener nema radnu smenu – praznik.",
    SICK_LEAVE:"Trener je nedostupan – bolovanje.",
    VACATION:"Trener je nedostupan – odmor.",
  };
  const mutedDateReasons=new Map<string,string>([
    ...rows.filter(row=>row.status!=="WORKING").map(row=>[row.date,unavailableReasons[row.status]||"Trener nema radnu smenu."] as [string,string]),
    ...holidays.map(holiday=>[holiday.date,`Neradan dan – praznik: ${holiday.description}`] as [string,string]),
  ]);
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
                      void saveGym(
                        day,
                        row?.openingTime?.slice(0, 5) ?? "07:00",
                        row?.closingTime?.slice(0, 5) ?? "22:00",
                        row?.id || undefined,
                      );
                    }}
                  >
                    <strong>{labels[day]}</strong>
                    <input
                      name="start"
                      type="time"
                      value={row?.openingTime?.slice(0, 5) ?? "07:00"}
                      onChange={(event) => setGym(current => row
                        ? current.map(item => item.day === day ? { ...item, openingTime: event.target.value + ":00" } : item)
                        : [...current, { id: 0, day, openingTime: event.target.value + ":00", closingTime: "22:00:00" }])}
                    />
                    <span>—</span>
                    <input
                      name="end"
                      type="time"
                      value={row?.closingTime?.slice(0, 5) ?? "22:00"}
                      onChange={(event) => setGym(current => row
                        ? current.map(item => item.day === day ? { ...item, closingTime: event.target.value + ":00" } : item)
                        : [...current, { id: 0, day, openingTime: "07:00:00", closingTime: event.target.value + ":00" }])}
                    />
                    <button>Sačuvaj</button>
                    {savedDay === day && <small className="save-indicator">Sačuvano ✓</small>}
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
                await holidayApi.create(holidayForm);
                setHolidays(await holidayApi.list());
                setHolidayForm(current => ({ ...current, description: "" }));
              }}
            >
              <input required name="date" type="date" value={holidayForm.date} onChange={event => setHolidayForm({ ...holidayForm, date: event.target.value })} />
              <input required name="description" placeholder="Opis praznika" value={holidayForm.description} onChange={event => setHolidayForm({ ...holidayForm, description: event.target.value })} />
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
      {own && <section className="progress-card trainer-schedule-card">
        <div className="card-head">
          <div>
            <p className="eyebrow">
              {own ? "Lični kalendar" : "Nadzor trenera"}
            </p>
            <h2>
              {own ? "Moje smene i odsustva" : "Raspored izabranog trenera"}
            </h2>
          </div>
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
        <div className={own?"calendar-list-layout":""}>{own&&<MonthCalendar value={selectedDate} onChange={setSelectedDate} highlightedDates={new Set(rows.map(row=>row.date))} mutedDates={new Set([...rows.filter(row=>row.status!=="WORKING").map(row=>row.date),...holidays.map(holiday=>holiday.date)])} mutedDateReasons={mutedDateReasons}/>}<div className="schedule-list">
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
              <div className="row-actions">{r.status === "WORKING" && <button onClick={() => void editShift(r)}>Izmeni</button>}<button className="icon-danger" onClick={async () => { await trainerScheduleApi.remove(r.id, true); await loadRows(); }}>×</button></div>
            </article>
          ))}
          {!rows.length && (
            <div className="empty-panel">Još nema unetih termina.</div>
          )}
        </div></div>
      </section>}
    </main>
  );
}
