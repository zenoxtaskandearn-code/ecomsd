import { Link } from 'react-router-dom';

const HERO_IMAGE = '/hero-dog.png';
const HERO_LAYOUT = '/behindloglayout.png';

export default function HeroSection() {
  return (
    <section className="bg-fetch-lavender">
      <div className="section-container">
        <div className="grid items-center gap-10 py-12 sm:py-14 md:min-h-[500px] md:grid-cols-[minmax(0,0.86fr)_minmax(0,1.14fr)] md:gap-10 md:py-14 lg:min-h-[540px] lg:gap-16 lg:py-16">
          <div className="mx-auto w-full max-w-[480px] text-center md:mx-0 md:max-w-[430px] md:text-left">
            <p className="text-[11px] font-medium uppercase tracking-[0.22em] text-fetch-ink/55">
              Premium dog accessories
            </p>
            <h1 className="mt-4 max-w-[12ch] text-[2.85rem] font-semibold leading-[0.98] tracking-[-0.03em] text-fetch-ink sm:text-[3.35rem] md:max-w-none lg:text-[3.9rem]">
              <span className="block">Sustainable</span>
              <span className="block text-fetch-ink/95">design for</span>
              <span className="block">modern dogs</span>
            </h1>
            <p className="mx-auto mt-5 max-w-[34ch] text-[15px] leading-[1.8] text-fetch-muted md:mx-0 sm:text-[17px]">
              Stylish and eco-friendly dog accessories for the conscious pup.
            </p>
            <div className="mt-8 flex flex-col items-center gap-4 sm:flex-row sm:justify-center md:justify-start">
              <Link to="/shop" className="btn-primary min-w-[188px]">
                Shop walking kits
              </Link>
              <Link
                to="/shop"
                className="inline-flex h-11 items-center justify-center text-[15px] font-medium text-fetch-ink/75 underline decoration-fetch-ink/15 underline-offset-[5px] transition-colors hover:text-fetch-ink hover:decoration-fetch-ink/40"
              >
                Browse essentials
              </Link>
            </div>
          </div>

          <div className="flex w-full items-center justify-center md:justify-end">
            <div className="relative isolate w-full max-w-[420px] sm:max-w-[480px] md:max-w-[540px] lg:max-w-[590px]">
              {/* Layout backdrop (behindloglayout.png: 755x593) sized relative to dog (1295x1215) */}
              <div className="pointer-events-none absolute inset-x-0 bottom-0 top-[6%] z-0 flex items-center justify-center" aria-hidden="true">
                <img
                  src={HERO_LAYOUT}
                  alt=""
                  className="h-full w-full select-none object-contain object-center"
                  loading="eager"
                  decoding="async"
                />
              </div>
              <img
                src={HERO_IMAGE}
                alt="Black dog wearing a premium beige collar"
                className="relative z-[1] block h-auto w-full object-contain"
                loading="eager"
                decoding="async"
              />
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
